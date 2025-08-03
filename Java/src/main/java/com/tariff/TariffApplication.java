package com.tariff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tariff.config.DatabaseConfig;
import com.tariff.model.CombinedTariff;
import com.tariff.service.TariffService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TariffApplication {
    private static final Logger logger = LoggerFactory.getLogger(TariffApplication.class);

    public static void main(String[] args) {
        DatabaseConfig config = new DatabaseConfig();
        TariffService service = new TariffService(config);
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        try {
            service.initialize();

            System.out.println("=== Single Operations ===");

            // Get Combined Tariff for an item
            CombinedTariff combinedTariff = service.calculateTotalTariff("ITEM-001", "CHN");
            System.out.println("Combined Tariffs for ITEM-001 in CHN: " +
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(combinedTariff));

            // Health check
            Map<String, String> health = service.healthCheck();
            System.out.println("Health check: " +
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(health));

        } catch (Exception e) {
            logger.error("Application error: {}", e.getMessage(), e);
        } finally {
            service.close();
        }
    }
}