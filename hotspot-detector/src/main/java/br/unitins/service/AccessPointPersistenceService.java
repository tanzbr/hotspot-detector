package br.unitins.service;

import br.unitins.database.DatabaseManager;
import br.unitins.model.AccessPoint;
import br.unitins.model.AccessPointEntity;
import br.unitins.repository.AccessPointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço simplificado de persistência para Access Points
 * Integra o escaneamento com o banco de dados MariaDB
 */
public class AccessPointPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccessPointPersistenceService.class);
    
    private final HotspotDetectorService hotspotDetectorService;
    private final AccessPointRepository repository;
    
    public AccessPointPersistenceService() {
        this.hotspotDetectorService = new HotspotDetectorService();
        this.repository = new AccessPointRepository();
        
        // Inicializa o banco de dados e testa a conexão
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        try {
            DatabaseManager databaseManager = DatabaseManager.getInstance();
            databaseManager.addShutdownHook();
            
            if (databaseManager.testConnection()) {
                logger.info("Conexão com banco de dados estabelecida com sucesso");
            } else {
                throw new RuntimeException("Falha ao conectar com o banco de dados");
            }
            
        } catch (Exception e) {
            logger.error("Erro ao inicializar banco de dados: {}", e.getMessage());
            throw new RuntimeException("Falha na inicialização do banco de dados", e);
        }
    }
    
    /**
     * Escaneia Access Points e persiste no banco de dados
     * @return Lista de Access Points escaneados e salvos
     */
    public List<AccessPointEntity> scanAndPersist() {
        try {
            logger.info("Iniciando escaneamento e persistência de Access Points...");
            
            // Escaneia Access Points
            List<AccessPoint> accessPoints = hotspotDetectorService.scanAccessPoints();
            
            if (accessPoints.isEmpty()) {
                logger.warn("Nenhum Access Point encontrado no escaneamento");
                return List.of();
            }
            
            // Converte para entidades
            List<AccessPointEntity> entities = accessPoints.stream()
                    .map(AccessPointEntity::new)
                    .collect(Collectors.toList());
            
            // Salva no banco de dados
            List<AccessPointEntity> savedEntities = repository.saveAll(entities);
            
            logger.info("Escaneamento concluído: {} Access Points salvos no banco de dados", 
                    savedEntities.size());
            
            return savedEntities;
            
        } catch (Exception e) {
            logger.error("Erro durante escaneamento e persistência: {}", e.getMessage());
            throw new RuntimeException("Falha no escaneamento e persistência", e);
        }
    }
    
    /**
     * Obtém os Access Points mais recentes do banco de dados
     * @return Lista de Access Points mais recentes
     */
    public List<AccessPoint> getLatestAccessPoints() {
        try {
            List<AccessPointEntity> entities = repository.findLatest();
            return entities.stream()
                    .map(AccessPointEntity::toAccessPoint)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erro ao buscar Access Points mais recentes: {}", e.getMessage());
            throw new RuntimeException("Falha ao buscar dados do banco", e);
        }
    }
    
    /**
     * Busca Access Points por período no banco de dados
     * @param startTime Data/hora inicial
     * @param endTime Data/hora final
     * @return Lista de Access Points encontrados
     */
    public List<AccessPoint> getAccessPointsByPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<AccessPointEntity> entities = repository.findByPeriod(startTime, endTime);
            return entities.stream()
                    .map(AccessPointEntity::toAccessPoint)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Erro ao buscar Access Points por período: {}", e.getMessage());
            throw new RuntimeException("Falha ao buscar dados do banco", e);
        }
    }
} 