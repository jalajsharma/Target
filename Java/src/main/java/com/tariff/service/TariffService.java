package com.tariff.service;

import com.tariff.cache.CacheManager;
import com.tariff.config.DatabaseConfig;
import com.tariff.connection.ConnectionPool;
import com.tariff.enums.TariffCombinationPolicy;
import com.tariff.model.CombinedTariff;
import com.tariff.model.Component;
import com.tariff.model.TariffRate;
import com.tariff.repository.TariffRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TariffService {
    private static final Logger logger = LoggerFactory.getLogger(TariffService.class);

    private final DatabaseConfig config;
    private final ConnectionPool connectionPool;
    private final TariffRepository repository;
    private final CacheManager cacheManager;
    private final ExecutorService executorService;

    public TariffService(DatabaseConfig config) {
        this.config = config;
        this.connectionPool = new ConnectionPool(config);
        this.repository = new TariffRepository(connectionPool);
        this.cacheManager = new CacheManager(connectionPool);
        this.executorService = Executors.newFixedThreadPool(20);
    }

    public void initialize() throws SQLException {
        connectionPool.initialize();
        logger.info("TariffService initialized successfully");
    }

    public List<Component> resolveBom(String itemId) throws SQLException {
        String cacheKey = cacheManager.generateCacheKey("bom", "resolveBom", itemId);

        // Try cache first
        @SuppressWarnings("unchecked")
        List<Component> cached = cacheManager.get(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }

        // Fetch from database
        List<Component> components = repository.resolveBom(itemId);

        // Cache the result
        cacheManager.set(cacheKey, components, config.getBomCacheTtl());

        return components;
    }

    public Optional<TariffRate> getEntityTariff(String entityId, String country) throws SQLException {
        String cacheKey = cacheManager.generateCacheKey("tariff", "getEntityTariff", entityId, country);

        // Try cache first
        TariffRate cached = cacheManager.get(cacheKey, TariffRate.class);
        if (cached != null) {
            return Optional.of(cached);
        }

        // Fetch from database
        Optional<TariffRate> tariff = repository.getEntityTariff(entityId, country);

        // Cache the result if present
        tariff.ifPresent(tariffRate -> cacheManager.set(cacheKey, tariffRate, config.getCacheTtl()));

        return tariff;
    }

    public Map<String, List<Component>> batchResolveBom(List<String> itemIds) {
        List<CompletableFuture<Map.Entry<String, List<Component>>>> futures = itemIds.stream()
                .map(itemId -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return Map.entry(itemId, resolveBom(itemId));
                    } catch (SQLException e) {
                        logger.error("Error resolving BOM for {}: {}", itemId, e.getMessage());
                        return Map.entry(itemId, Collections.<Component>emptyList());
                    }
                }, executorService))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, Optional<TariffRate>> batchGetEntityTariff(List<Map.Entry<String, String>> requests) {
        List<CompletableFuture<Map.Entry<String, Optional<TariffRate>>>> futures = requests.stream()
                .map(request -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String key = request.getKey() + "_" + request.getValue();
                        return Map.entry(key, getEntityTariff(request.getKey(), request.getValue()));
                    } catch (SQLException e) {
                        logger.error("Error getting tariff for {}, {}: {}",
                                request.getKey(), request.getValue(), e.getMessage());
                        return Map.entry(request.getKey() + "_" + request.getValue(),
                                Optional.<TariffRate>empty());
                    }
                }, executorService))
                .collect(Collectors.toList());

        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public TariffCombinationPolicy getCombinationPolicy(String policyVersionId) throws SQLException {
        return repository.getCombinationPolicy(policyVersionId);
    }

    public TariffCalculationResult combineTariff(Optional<TariffRate> itemTariff,
                                                 Map<String, TariffRate> componentTariffs,
                                                 TariffCombinationPolicy policy) {

        BigDecimal itemRate = itemTariff.map(t -> BigDecimal.valueOf(t.getTariffRate()))
                .orElse(BigDecimal.ZERO);

        BigDecimal componentRate = componentTariffs.values().stream()
                .map(t -> BigDecimal.valueOf(t.getTariffRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalRate = switch (policy) {
            case MAXIMUM -> itemRate.max(componentRate);
            case MINIMUM -> itemRate.min(componentRate);
            case ITEM -> itemRate.compareTo(BigDecimal.ZERO) > 0 ? itemRate : componentRate;
            case COMPONENT -> componentRate;
            default -> itemRate.add(componentRate);
        };

        return new TariffCalculationResult(itemRate, componentRate, finalRate);
    }

    public CombinedTariff calculateTotalTariff(String itemId, String country) throws SQLException {
        String cacheKey = cacheManager.generateCacheKey("calculatedTariff", "calculateTotalTariff", itemId, country);

        // Try cache first
        CombinedTariff cached = cacheManager.get(cacheKey, CombinedTariff.class);
        if (cached != null) {
            return cached;
        }

        logger.info("Starting tariff calculation item_id={}, country={}", itemId, country);

        try {
            // Step 1: Resolve BOM and get item tariff in parallel
            CompletableFuture<List<Component>> bomFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return resolveBom(itemId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

            CompletableFuture<Optional<TariffRate>> itemTariffFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return getEntityTariff(itemId, country);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }, executorService);

            List<Component> components = bomFuture.join();
            Optional<TariffRate> itemTariff = itemTariffFuture.join();

            logger.info("BOM and item tariff resolved item_id={}, components_count={}", itemId, components.size());

            // Step 2: Fetch component tariffs in batch
            List<Map.Entry<String, String>> batchRequests = components.stream()
                    .map(comp -> Map.entry(comp.getComponentId(), country))
                    .collect(Collectors.toList());

            Map<String, Optional<TariffRate>> componentTariffResults = batchGetEntityTariff(batchRequests);

            Map<String, TariffRate> componentTariffs = componentTariffResults.entrySet().stream()
                    .filter(entry -> entry.getValue().isPresent())
                    .collect(Collectors.toMap(
                            entry -> entry.getKey().split("_")[0], // Extract component ID
                            entry -> entry.getValue().get()
                    ));

            logger.info("Component tariffs fetched, item_id={}, components_with_tariffs={}",
                    itemId, componentTariffs.size());

            // Step 3: Get combination policy
            String policyVersionId = itemTariff.map(TariffRate::getPolicyVersionId)
                    .orElse(componentTariffs.values().stream()
                            .findFirst()
                            .map(TariffRate::getPolicyVersionId)
                            .orElse(null));

            TariffCombinationPolicy combinationPolicy = policyVersionId != null ?
                    getCombinationPolicy(policyVersionId) : TariffCombinationPolicy.ADDITIVE;

            // Step 4: Combine tariffs
            TariffCalculationResult calculationResult = combineTariff(itemTariff, componentTariffs, combinationPolicy);

            // Step 5: Create result
            CombinedTariff result = new CombinedTariff(
                    itemId,
                    country,
                    calculationResult.getItemRate(),
                    calculationResult.getComponentRate(),
                    calculationResult.getFinalRate(),
                    combinationPolicy,
                    new ArrayList<>(componentTariffs.keySet()),
                    LocalDateTime.now()
            );

            // Cache the result
            cacheManager.set(cacheKey, result, config.getCacheTtl());

            logger.info("Tariff calculation completed, item_id={}, country={}, final_rate={}, policy={}",
                    itemId, country, calculationResult.getFinalRate(), combinationPolicy.getValue());

            return result;

        } catch (Exception e) {
            logger.error("Tariff calculation failed, item_id={}, country={}, error={}",
                    itemId, country, e.getMessage());
            throw new SQLException("Tariff calculation failed", e);
        }
    }

    public Map<String, String> healthCheck() {
        Map<String, String> health = new HashMap<>();
        try {
            // Check database connection
            connectionPool.getConnection().close();

            // Check Redis connection
            cacheManager.get("health_check", String.class);

            health.put("status", "healthy");
            health.put("timestamp", LocalDateTime.now().toString());
        } catch (Exception e) {
            logger.error("Health check failed: {}", e.getMessage());
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now().toString());
        }
        return health;
    }

    public void close() {
        connectionPool.close();
        executorService.shutdown();
        logger.info("TariffService closed successfully");
    }

    // Inner class for tariff calculation results
    public static class TariffCalculationResult {
        private final BigDecimal itemRate;
        private final BigDecimal componentRate;
        private final BigDecimal finalRate;

        public TariffCalculationResult(BigDecimal itemRate, BigDecimal componentRate, BigDecimal finalRate) {
            this.itemRate = itemRate;
            this.componentRate = componentRate;
            this.finalRate = finalRate;
        }

        public BigDecimal getItemRate() { return itemRate; }
        public BigDecimal getComponentRate() { return componentRate; }
        public BigDecimal getFinalRate() { return finalRate; }
    }
}