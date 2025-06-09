package br.unitins.scheduler;

import br.unitins.service.AccessPointPersistenceService;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scheduler simplificado para escaneamento automático de Access Points
 */
public class AccessPointScheduler {
    
    private static final Logger logger = LoggerFactory.getLogger(AccessPointScheduler.class);
    private static final String SCAN_JOB_NAME = "AccessPointScanJob";
    private static final String SCAN_TRIGGER_NAME = "AccessPointScanTrigger";
    private static final String GROUP_NAME = "AccessPointGroup";
    
    private Scheduler scheduler;
    
    public AccessPointScheduler() {
        try {
            this.scheduler = StdSchedulerFactory.getDefaultScheduler();
        } catch (SchedulerException e) {
            logger.error("Erro ao inicializar scheduler: {}", e.getMessage());
            throw new RuntimeException("Falha na inicialização do scheduler", e);
        }
    }
    
    /**
     * Inicia o scheduler com escaneamento a cada 1 minuto
     */
    public void start() throws SchedulerException {
        if (scheduler.isStarted()) {
            logger.warn("Scheduler já está iniciado");
            return;
        }
        
        // Define o job de escaneamento
        JobDetail scanJob = JobBuilder.newJob(ScanJob.class)
                .withIdentity(SCAN_JOB_NAME, GROUP_NAME)
                .build();
        
        // Define o trigger para executar a cada 1 minuto
        Trigger scanTrigger = TriggerBuilder.newTrigger()
                .withIdentity(SCAN_TRIGGER_NAME, GROUP_NAME)
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMinutes(1)
                        .repeatForever())
                .build();
        
        // Agenda o job
        scheduler.scheduleJob(scanJob, scanTrigger);
        scheduler.start();
        
        logger.info("Scheduler iniciado - escaneamento automático a cada 1 minuto");
    }
    
    /**
     * Para o scheduler
     */
    public void stop() throws SchedulerException {
        if (scheduler != null && scheduler.isStarted()) {
            scheduler.shutdown(true);
            logger.info("Scheduler parado");
        }
    }
    
    /**
     * Verifica se o scheduler está rodando
     */
    public boolean isRunning() throws SchedulerException {
        return scheduler != null && scheduler.isStarted() && !scheduler.isShutdown();
    }
    
    /**
     * Adiciona hook de shutdown para parar o scheduler graciosamente
     */
    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (isRunning()) {
                    stop();
                }
            } catch (SchedulerException e) {
                logger.error("Erro ao parar scheduler no shutdown: {}", e.getMessage());
            }
        }));
    }
    
    /**
     * Job interno para execução do escaneamento
     */
    public static class ScanJob implements Job {
        private static final Logger jobLogger = LoggerFactory.getLogger(ScanJob.class);
        
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            try {
                AccessPointPersistenceService service = new AccessPointPersistenceService();
                service.scanAndPersist();
                
            } catch (Exception e) {
                jobLogger.error("Erro durante escaneamento automático: {}", e.getMessage());
                throw new JobExecutionException("Falha no escaneamento automático", e);
            }
        }
    }
} 