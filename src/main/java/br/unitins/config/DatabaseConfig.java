package br.unitins.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

/**
 * Configuração simplificada do banco de dados
 */
public class DatabaseConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static DatabaseConfig instance;
    
    // Configurações do banco
    private String host = "localhost";
    private int port = 3306;
    private String name = "hotspot_detector";
    private String username = "hotspot_user";
    private String password = "hotspot_pass";
    
    private DatabaseConfig() {
        loadConfig();
    }
    
    public static synchronized DatabaseConfig getInstance() {
        if (instance == null) {
            instance = new DatabaseConfig();
        }
        return instance;
    }
    
    @SuppressWarnings("unchecked")
    private void loadConfig() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("database.yml")) {
            if (inputStream == null) {
                logger.warn("Arquivo database.yml não encontrado. Usando configurações padrão.");
                return;
            }
            
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            Map<String, Object> config = mapper.readValue(inputStream, Map.class);
            
            Map<String, Object> database = (Map<String, Object>) config.get("database");
            if (database != null) {
                this.host = (String) database.getOrDefault("host", this.host);
                this.port = (Integer) database.getOrDefault("port", this.port);
                this.name = (String) database.getOrDefault("name", this.name);
                this.username = (String) database.getOrDefault("username", this.username);
                this.password = (String) database.getOrDefault("password", this.password);
            }
            
            logger.info("Configuração carregada: {}:{}/{}", host, port, name);
            
        } catch (Exception e) {
            logger.warn("Erro ao carregar configuração. Usando valores padrão: {}", e.getMessage());
        }
    }
    
    // Getters
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    
    public String getJdbcUrl() {
        return String.format("jdbc:mariadb://%s:%d/%s", host, port, name);
    }
} 