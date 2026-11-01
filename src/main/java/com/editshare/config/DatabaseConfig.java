package com.editshare.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);

    static {
        // Load local untracked .env file if present (keeps secrets safe from git)
        java.io.File envFile = new java.io.File(".env");
        if (envFile.exists() && envFile.isFile()) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#") && line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        String key = parts[0].trim();
                        String val = parts[1].trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Value("${spring.datasource.password:}")
    private String datasourcePassword;

    @Value("${spring.datasource.driver-class-name:org.postgresql.Driver}")
    private String driverClassName;

    @Value("${spring.datasource.hikari.maximum-pool-size:25}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:5}")
    private int minIdle;

    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName(driverClassName);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(20000);
        config.setIdleTimeout(300000);
        config.setMaxLifetime(1200000);

        String url = datasourceUrl != null ? datasourceUrl.trim() : "";
        String user = datasourceUsername;
        String pass = datasourcePassword;

        // Support cloud platforms (Render, Supabase, Neon, Railway) that provide postgres:// or postgresql:// URIs
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            try {
                // Normalize URI prefix for java.net.URI parsing
                String parseUri = url.startsWith("postgres://") ? "postgresql://" + url.substring("postgres://".length()) : url;
                URI uri = new URI(parseUri);
                String host = uri.getHost();
                int port = uri.getPort() != -1 ? uri.getPort() : 5432;
                String path = uri.getPath() != null && uri.getPath().length() > 1 ? uri.getPath().substring(1) : "prathi";
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    user = parts[0];
                    pass = parts[1];
                }
                url = String.format("jdbc:postgresql://%s:%d/%s", host, port, path);
                log.info("Converted raw PostgreSQL connection string to JDBC format for host: {}:{}", host, port);
            } catch (Exception e) {
                log.warn("Failed to parse raw PostgreSQL URI, using raw URL: {}", e.getMessage());
            }
        }

        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(pass);

        return new HikariDataSource(config);
    }
}
