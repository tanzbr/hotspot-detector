package br.unitins.database;

import br.unitins.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gerenciador simplificado do banco de dados
 */
public class DatabaseManager {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static DatabaseManager instance;
    
    private HikariDataSource dataSource;
    private EntityManagerFactory entityManagerFactory;
    private final DatabaseConfig config;
    
    private DatabaseManager() {
        this.config = DatabaseConfig.getInstance();
        initializeDataSource();
        initializeJPA();
    }
    
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }
    
    private void initializeDataSource() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            
            // Configurações básicas
            hikariConfig.setJdbcUrl(config.getJdbcUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
            hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");
            
            // Configurações padrão do pool
            hikariConfig.setMaximumPoolSize(10);
            hikariConfig.setMinimumIdle(2);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(600000);
            hikariConfig.setMaxLifetime(1800000);
            
            this.dataSource = new HikariDataSource(hikariConfig);
            logger.info("Pool de conexões inicializado");
            
        } catch (Exception e) {
            logger.error("Erro ao inicializar pool de conexões: {}", e.getMessage());
            throw new RuntimeException("Falha na inicialização do pool de conexões", e);
        }
    }
    
    private void initializeJPA() {
        try {
            Map<String, Object> properties = new HashMap<>();
            
            // Configurações básicas do Hibernate
            properties.put("hibernate.dialect", "org.hibernate.dialect.MariaDBDialect");
            properties.put("hibernate.hbm2ddl.auto", "update");
            properties.put("hibernate.show_sql", false);
            properties.put("hibernate.format_sql", true);
            
            // Configurações de conexão
            properties.put("hibernate.connection.datasource", dataSource);
            
            // Usar CustomPersistenceUnitInfo
            CustomPersistenceUnitInfo persistenceUnitInfo = new CustomPersistenceUnitInfo("hotspot-detector");
            
            this.entityManagerFactory = new HibernatePersistenceProvider()
                    .createContainerEntityManagerFactory(persistenceUnitInfo, properties);
            
            logger.info("JPA inicializado");
            
        } catch (Exception e) {
            logger.error("Erro ao inicializar JPA: {}", e.getMessage());
            throw new RuntimeException("Falha na inicialização do JPA", e);
        }
    }
    
    public EntityManager getEntityManager() {
        return entityManagerFactory.createEntityManager();
    }
    
    public DataSource getDataSource() {
        return dataSource;
    }
    
    public boolean testConnection() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(5);
        } catch (SQLException e) {
            logger.error("Erro ao testar conexão: {}", e.getMessage());
            return false;
        }
    }
    
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (Exception e) {
                logger.error("Erro durante shutdown: {}", e.getMessage());
            }
        }));
    }
    
    public void shutdown() {
        try {
            if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
                entityManagerFactory.close();
                logger.info("EntityManagerFactory fechado");
            }
            
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("Pool de conexões fechado");
            }
        } catch (Exception e) {
            logger.error("Erro durante shutdown: {}", e.getMessage());
        }
    }
} 