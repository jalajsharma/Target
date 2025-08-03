package com.tariff.config;

public class DatabaseConfig {
    private String dbHost;
    private int dbPort;
    private String dbName;
    private String dbUser;
    private String dbPassword;
    private int dbPoolMinSize;
    private int dbPoolMaxSize;

    private String redisHost;
    private int redisPort;
    private int redisDb;
    private String redisPassword;

    private int cacheTtl;
    private int bomCacheTtl;

    public DatabaseConfig() {
        this.dbHost = System.getProperty("DB_HOST", "localhost");
        this.dbPort = Integer.parseInt(System.getProperty("DB_PORT", "5432"));
        this.dbName = System.getProperty("DB_NAME", "tariff_management_system");
        this.dbUser = System.getProperty("DB_USER", "postgres");
        this.dbPassword = System.getProperty("DB_PASSWORD", "postgres");
        this.dbPoolMinSize = Integer.parseInt(System.getProperty("DB_POOL_MIN_SIZE", "10"));
        this.dbPoolMaxSize = Integer.parseInt(System.getProperty("DB_POOL_MAX_SIZE", "50"));

        this.redisHost = System.getProperty("REDIS_HOST", "localhost");
        this.redisPort = Integer.parseInt(System.getProperty("REDIS_PORT", "6379"));
        this.redisDb = Integer.parseInt(System.getProperty("REDIS_DB", "0"));
        this.redisPassword = System.getProperty("REDIS_PASSWORD");

        this.cacheTtl = Integer.parseInt(System.getProperty("CACHE_TTL", "3600"));
        this.bomCacheTtl = Integer.parseInt(System.getProperty("BOM_CACHE_TTL", "7200"));
    }

    // Getters
    public String getDbHost() { return dbHost; }
    public int getDbPort() { return dbPort; }
    public String getDbName() { return dbName; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public int getDbPoolMinSize() { return dbPoolMinSize; }
    public int getDbPoolMaxSize() { return dbPoolMaxSize; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public int getRedisDb() { return redisDb; }
    public String getRedisPassword() { return redisPassword; }
    public int getCacheTtl() { return cacheTtl; }
    public int getBomCacheTtl() { return bomCacheTtl; }
}
