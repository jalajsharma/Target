package com.tariff.connection;

import com.tariff.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionPool {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    private final DatabaseConfig config;
    private HikariDataSource dataSource;
    private JedisPool jedisPool;

    public ConnectionPool(DatabaseConfig config) {
        this.config = config;
    }

    public void initialize() throws SQLException {
        try {
            initializePostgreSQL();
            initializeRedis();
            logger.info("Database and Redis connections initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize connections: {}", e.getMessage(), e);
            throw new SQLException("Connection initialization failed", e);
        }
    }

    private void initializePostgreSQL() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(String.format("jdbc:postgresql://%s:%d/%s",
                config.getDbHost(), config.getDbPort(), config.getDbName()));
        hikariConfig.setUsername(config.getDbUser());
        hikariConfig.setPassword(config.getDbPassword());
        hikariConfig.setMinimumIdle(config.getDbPoolMinSize());
        hikariConfig.setMaximumPoolSize(config.getDbPoolMaxSize());
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setLeakDetectionThreshold(60000);

        this.dataSource = new HikariDataSource(hikariConfig);
    }

    private void initializeRedis() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(100);
        poolConfig.setMaxIdle(50);
        poolConfig.setMinIdle(10);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);

        if (config.getRedisPassword() != null && !config.getRedisPassword().isEmpty()) {
            this.jedisPool = new JedisPool(poolConfig, config.getRedisHost(),
                    config.getRedisPort(), 2000, config.getRedisPassword(), config.getRedisDb());
        } else {
            this.jedisPool = new JedisPool(poolConfig, config.getRedisHost(),
                    config.getRedisPort(), 2000, null, config.getRedisDb());
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public JedisPool getJedisPool() {
        return jedisPool;
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
    }
}