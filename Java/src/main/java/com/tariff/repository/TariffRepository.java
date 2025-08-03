package com.tariff.repository;

import com.tariff.model.Component;
import com.tariff.model.TariffRate;
import com.tariff.enums.TariffCombinationPolicy;
import com.tariff.connection.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TariffRepository {
    private static final Logger logger = LoggerFactory.getLogger(TariffRepository.class);
    private final ConnectionPool connectionPool;

    public TariffRepository(ConnectionPool connectionPool) {
        this.connectionPool = connectionPool;
    }

    public List<Component> resolveBom(String itemId) throws SQLException {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("item_id cannot be empty");
        }

        String query = """
            SELECT c.component_id, c.description, c.material_type
            FROM Component c
            INNER JOIN Item_Component ic ON c.component_id = ic.component_id
            WHERE ic.item_id = ?
            ORDER BY c.component_id
        """;

        List<Component> components = new ArrayList<>();

        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, itemId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Component component = new Component(
                            rs.getString("component_id"),
                            rs.getString("description"),
                            rs.getString("material_type")
                    );
                    components.add(component);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in resolveBom for item {}: {}", itemId, e.getMessage());
            throw e;
        }

        logger.info("Resolved BOM for item {}: {} components", itemId, components.size());
        return components;
    }

    public Optional<TariffRate> getEntityTariff(String entityId, String country) throws SQLException {
        if (entityId == null || entityId.trim().isEmpty()) {
            throw new IllegalArgumentException("entity_id cannot be empty");
        }
        if (country == null || country.trim().isEmpty() || country.length() != 3) {
            throw new IllegalArgumentException("country must be a valid 3-character country code");
        }

        country = country.toUpperCase();

        String query = """
            SELECT
                t.tariff_id::text,
                t.tariff_rate,
                t.level,
                t.entity_id,
                t.country_code,
                t.start_date,
                t.end_date,
                t.status,
                t.policy_version_id
            FROM TariffRule t
            INNER JOIN PolicyVersion p ON t.policy_version_id = p.policy_version_id
            WHERE t.entity_id = ?
                AND t.country_code = ?
                AND t.status = 'ACTIVE'
                AND t.start_date <= CURRENT_DATE
                AND (t.end_date IS NULL OR t.end_date >= CURRENT_DATE)
                AND p.start_date <= CURRENT_DATE
                AND (p.end_date IS NULL OR p.end_date >= CURRENT_DATE)
            ORDER BY t.level, t.start_date DESC
            LIMIT 1
        """;

        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, entityId);
            stmt.setString(2, country);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    TariffRate tariff = new TariffRate(
                            rs.getString("tariff_id"),
                            rs.getDouble("tariff_rate"),
                            rs.getString("level"),
                            rs.getString("entity_id"),
                            rs.getString("country_code"),
                            rs.getObject("start_date", LocalDate.class),
                            rs.getObject("end_date", LocalDate.class),
                            rs.getString("status"),
                            rs.getString("policy_version_id")
                    );

                    logger.info("Retrieved tariff for entity {} in {}", entityId, country);
                    return Optional.of(tariff);
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in getEntityTariff for entity {}, country {}: {}",
                    entityId, country, e.getMessage());
            throw e;
        }

        return Optional.empty();
    }

    public TariffCombinationPolicy getCombinationPolicy(String policyVersionId) throws SQLException {
        if (policyVersionId == null || policyVersionId.trim().isEmpty()) {
            throw new IllegalArgumentException("policy_version_id cannot be empty");
        }
        UUID uuid_policyVersionId;
        try {
            uuid_policyVersionId = UUID.fromString(policyVersionId);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("policy_version_id is not a valid uuid");
        }

        String query = """
            SELECT tariff_combination_policy
            FROM policyversion pv
            WHERE pv.policy_version_id = ?
                AND pv.start_date <= CURRENT_TIMESTAMP
                AND (pv.end_date IS NULL OR pv.end_date > CURRENT_TIMESTAMP)
            LIMIT 1
        """;

        try (Connection conn = connectionPool.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setObject(1, uuid_policyVersionId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String policyStr = rs.getString("tariff_combination_policy").toLowerCase();

                    if (policyStr.contains("additive")) {
                        return TariffCombinationPolicy.ADDITIVE;
                    } else if (policyStr.contains("maximum") || policyStr.contains("max")) {
                        return TariffCombinationPolicy.MAXIMUM;
                    } else if (policyStr.contains("minimum") || policyStr.contains("min")) {
                        return TariffCombinationPolicy.MINIMUM;
                    } else if (policyStr.contains("item")) {
                        return TariffCombinationPolicy.ITEM;
                    } else if (policyStr.contains("component")) {
                        return TariffCombinationPolicy.COMPONENT;
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Database error in getCombinationPolicy for policy version {}: {}",
                    policyVersionId, e.getMessage());
            throw e;
        }

        logger.info("Using default ADDITIVE policy for policy_version_id: {}", policyVersionId);
        return TariffCombinationPolicy.ADDITIVE;
    }
}