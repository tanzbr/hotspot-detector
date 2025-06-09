package br.unitins.repository;

import br.unitins.database.DatabaseManager;
import br.unitins.model.AccessPointEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Repositório simplificado para operações de Access Points no banco de dados
 */
public class AccessPointRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(AccessPointRepository.class);
    private static final int BATCH_SIZE = 25;
    
    /**
     * Salva uma lista de Access Points no banco de dados
     * @param entities Lista de entidades para salvar
     * @return Lista de entidades salvas
     */
    public List<AccessPointEntity> saveAll(List<AccessPointEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }
        
        EntityManager em = DatabaseManager.getInstance().getEntityManager();
        EntityTransaction transaction = em.getTransaction();
        List<AccessPointEntity> savedEntities = new ArrayList<>();
        
        try {
            transaction.begin();
            
            for (int i = 0; i < entities.size(); i++) {
                AccessPointEntity entity = entities.get(i);
                entity.setScanTime(LocalDateTime.now());
                em.persist(entity);
                savedEntities.add(entity);
                
                // Flush em lotes para melhor performance
                if (i % BATCH_SIZE == 0) {
                    em.flush();
                    em.clear();
                }
            }
            
            transaction.commit();
            
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            logger.error("Erro ao salvar Access Points: {}", e.getMessage());
            throw new RuntimeException("Falha ao salvar no banco de dados", e);
        } finally {
            em.close();
        }
        
        return savedEntities;
    }
    
    /**
     * Busca os Access Points mais recentes (últimos 5 minutos)
     * @return Lista de Access Points mais recentes
     */
    public List<AccessPointEntity> findLatest() {
        EntityManager em = DatabaseManager.getInstance().getEntityManager();
        
        try {
            LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
            
            TypedQuery<AccessPointEntity> query = em.createQuery(
                "SELECT DISTINCT ap FROM AccessPointEntity ap " +
                "WHERE ap.scanTime >= :cutoffTime " +
                "ORDER BY ap.scanTime DESC, ap.ssid ASC",
                AccessPointEntity.class
            );
            
            query.setParameter("cutoffTime", fiveMinutesAgo);
            query.setMaxResults(100); // Limita a 100 resultados
            
            List<AccessPointEntity> results = query.getResultList();
            
            return results;
            
        } catch (Exception e) {
            logger.error("Erro ao buscar Access Points mais recentes: {}", e.getMessage());
            throw new RuntimeException("Falha na consulta ao banco", e);
        } finally {
            em.close();
        }
    }
    
    /**
     * Busca Access Points por período
     * @param startTime Data/hora inicial
     * @param endTime Data/hora final
     * @return Lista de Access Points encontrados
     */
    public List<AccessPointEntity> findByPeriod(LocalDateTime startTime, LocalDateTime endTime) {
        EntityManager em = DatabaseManager.getInstance().getEntityManager();
        
        try {
            TypedQuery<AccessPointEntity> query = em.createQuery(
                "SELECT ap FROM AccessPointEntity ap " +
                "WHERE ap.scanTime BETWEEN :startTime AND :endTime " +
                "ORDER BY ap.scanTime DESC, ap.ssid ASC",
                AccessPointEntity.class
            );
            
            query.setParameter("startTime", startTime);
            query.setParameter("endTime", endTime);
            query.setMaxResults(500); // Limita a 500 resultados
            
            List<AccessPointEntity> results = query.getResultList();
            
            return results;
            
        } catch (Exception e) {
            logger.error("Erro ao buscar Access Points por período: {}", e.getMessage());
            throw new RuntimeException("Falha na consulta ao banco", e);
        } finally {
            em.close();
        }
    }
} 