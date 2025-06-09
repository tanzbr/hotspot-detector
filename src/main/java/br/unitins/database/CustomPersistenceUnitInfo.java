package br.unitins.database;

import br.unitins.model.AccessPointEntity;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;

import javax.sql.DataSource;
import java.net.URL;
import java.util.List;
import java.util.Properties;

/**
 * Configuração simplificada da unidade de persistência
 */
public class CustomPersistenceUnitInfo implements PersistenceUnitInfo {
    
    private final String persistenceUnitName;
    
    public CustomPersistenceUnitInfo(String persistenceUnitName) {
        this.persistenceUnitName = persistenceUnitName;
    }
    
    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }
    
    @Override
    public String getPersistenceProviderClassName() {
        return "org.hibernate.jpa.HibernatePersistenceProvider";
    }
    
    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return PersistenceUnitTransactionType.RESOURCE_LOCAL;
    }
    
    @Override
    public List<String> getManagedClassNames() {
        return List.of(AccessPointEntity.class.getName());
    }
    
    // Métodos não utilizados - implementação mínima
    @Override public DataSource getJtaDataSource() { return null; }
    @Override public DataSource getNonJtaDataSource() { return null; }
    @Override public List<String> getMappingFileNames() { return List.of(); }
    @Override public List<URL> getJarFileUrls() { return List.of(); }
    @Override public URL getPersistenceUnitRootUrl() { return null; }
    @Override public boolean excludeUnlistedClasses() { return false; }
    @Override public SharedCacheMode getSharedCacheMode() { return SharedCacheMode.UNSPECIFIED; }
    @Override public ValidationMode getValidationMode() { return ValidationMode.AUTO; }
    @Override public Properties getProperties() { return new Properties(); }
    @Override public String getPersistenceXMLSchemaVersion() { return "3.0"; }
    @Override public ClassLoader getClassLoader() { return Thread.currentThread().getContextClassLoader(); }
    @Override public void addTransformer(ClassTransformer transformer) {}
    @Override public ClassLoader getNewTempClassLoader() { return null; }
} 