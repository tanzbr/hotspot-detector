package br.unitins;

import br.unitins.model.AccessPoint;
import br.unitins.service.AccessPointPersistenceService;
import br.unitins.scheduler.AccessPointScheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Aplicação principal para detecção de Access Points Wi-Fi
 * Sistema simplificado com monitoramento em tempo real e consulta por data
 */
public class Main {
    
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("HOTSPOT DETECTOR - Monitor de Access Points Wi-Fi");
        System.out.println("Universidade Estadual do Tocantins - UNITINS");
        System.out.println("=".repeat(60));
        
        try {
            // Inicializa os serviços
            AccessPointPersistenceService persistenceService = new AccessPointPersistenceService();
            AccessPointScheduler scheduler = new AccessPointScheduler();
            
            // Inicia o scheduler automaticamente
            scheduler.start();
            scheduler.addShutdownHook();
            
            System.out.println("Sistema iniciado! Escaneamento automático ativo (1 minuto).");
            
            // Modo interativo
            runInteractiveMode(persistenceService, scheduler);
            
        } catch (Exception e) {
            logger.error("Erro na aplicação: {}", e.getMessage());
            System.err.println("\nErro: " + e.getMessage());
            System.err.println("\nPossíveis soluções:");
            System.err.println("- Execute como administrador/root");
            System.err.println("- Verifique se o Wi-Fi está habilitado");
            System.err.println("- Verifique se o MariaDB está rodando");
            System.err.println("- Verifique as configurações em database.yml");
            System.exit(1);
        }
    }
    
    private static void runInteractiveMode(AccessPointPersistenceService persistenceService, 
                                          AccessPointScheduler scheduler) throws Exception {
        Scanner scanner = new Scanner(System.in);
        
        while (true) {
            showMenu();
            System.out.print("Escolha uma opção: ");
            
            String choice = scanner.nextLine().trim();
            
            switch (choice) {
                case "1":
                    performRealTimeMonitoring(persistenceService, scanner);
                    break;
                    
                case "2":
                    performDateSearch(persistenceService, scanner);
                    break;
                    
                case "3":
                    System.out.println("Encerrando aplicação...");
                    try {
                        if (scheduler.isRunning()) {
                            scheduler.stop();
                        }
                    } catch (SchedulerException e) {
                        System.err.println("Erro ao parar scheduler: " + e.getMessage());
                    }
                    return;
                    
                default:
                    System.err.println("Opção inválida. Tente novamente.");
            }
        }
    }
    
    private static void showMenu() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("MENU PRINCIPAL");
        System.out.println("=".repeat(50));
        System.out.println("1. Monitoramento em tempo real");
        System.out.println("2. Pesquisar data específica");
        System.out.println("3. Sair");
        System.out.println("=".repeat(50));
    }
    
    private static void performRealTimeMonitoring(AccessPointPersistenceService persistenceService, 
                                                 Scanner scanner) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("MONITORAMENTO EM TEMPO REAL");
        System.out.println("Pressione ENTER para atualizar | Digite 'voltar' para sair");
        System.out.println("=".repeat(60));
        
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        
        // Atualização automática a cada 1 minuto
        executor.scheduleAtFixedRate(() -> {
            try {
                displayCurrentHotspots(persistenceService);
            } catch (Exception e) {
                System.err.println("Erro na atualização automática: " + e.getMessage());
            }
        }, 0, 1, TimeUnit.MINUTES);
        
        try {
            while (true) {
                String input = scanner.nextLine().trim().toLowerCase();
                
                if ("voltar".equals(input)) {
                    break;
                } else {
                    // Atualização manual ao pressionar ENTER
                    displayCurrentHotspots(persistenceService);
                }
            }
        } finally {
            executor.shutdown();
        }
    }
    
    private static void displayCurrentHotspots(AccessPointPersistenceService persistenceService) {
        try {
            List<AccessPoint> accessPoints = persistenceService.getLatestAccessPoints();
            
            // Limpa a tela (funciona na maioria dos terminais)
            System.out.print("\033[2J\033[H");
            
            System.out.println("=".repeat(80));
            System.out.println("HOTSPOTS DETECTADOS - " + LocalDateTime.now().format(DATE_FORMATTER));
            System.out.println("=".repeat(80));
            
            if (accessPoints.isEmpty()) {
                System.out.println("Nenhum Access Point detectado ainda.");
                System.out.println("Aguarde o próximo escaneamento...");
            } else {
                System.out.printf("%-20s %-17s %-10s %-12s %-8s %-10s %-15s%n",
                        "SSID", "MAC Address", "Qualidade", "Sinal(dBm)", "Canal", "Freq(GHz)", "Segurança");
                System.out.println("-".repeat(80));
                
                for (AccessPoint ap : accessPoints) {
                    System.out.printf("%-20s %-17s %-10.1f %-12d %-8d %-10.3f %-15s%n",
                            truncate(ap.getSsid() != null ? ap.getSsid() : "N/A", 20),
                            ap.getMacAddress() != null ? ap.getMacAddress() : "N/A",
                            ap.getLinkQuality(),
                            ap.getSignalLevel(),
                            ap.getChannel(),
                            ap.getFrequency(),
                            truncate(ap.getSecurityVersion() != null ? ap.getSecurityVersion() : "N/A", 15)
                    );
                }
                
                System.out.println("-".repeat(80));
                System.out.println("Total: " + accessPoints.size() + " Access Points únicos detectados");
            }
            
            System.out.println("\nPressione ENTER para atualizar | Digite 'voltar' para sair");
            
        } catch (Exception e) {
            System.err.println("Erro ao buscar dados: " + e.getMessage());
        }
    }
    
    private static void performDateSearch(AccessPointPersistenceService persistenceService, 
                                         Scanner scanner) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("PESQUISAR POR DATA ESPECÍFICA");
        System.out.println("=".repeat(60));
        
        try {
            System.out.println("Digite a data e hora inicial (formato: dd/MM/yyyy HH:mm):");
            System.out.print("Exemplo: 15/12/2024 14:30 > ");
            String startDateStr = scanner.nextLine().trim();
            
            if (startDateStr.isEmpty()) {
                System.out.println("Data não informada. Voltando ao menu principal.");
                return;
            }
            
            System.out.println("Digite a data e hora final (formato: dd/MM/yyyy HH:mm):");
            System.out.print("Exemplo: 15/12/2024 15:30 > ");
            String endDateStr = scanner.nextLine().trim();
            
            if (endDateStr.isEmpty()) {
                System.out.println("Data não informada. Voltando ao menu principal.");
                return;
            }
            
            LocalDateTime startDate = LocalDateTime.parse(startDateStr, DATE_FORMATTER);
            LocalDateTime endDate = LocalDateTime.parse(endDateStr, DATE_FORMATTER);
            
            if (startDate.isAfter(endDate)) {
                System.out.println("Data inicial deve ser anterior à data final!");
                return;
            }
            
            List<AccessPoint> accessPoints = persistenceService.getAccessPointsByPeriod(startDate, endDate);
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("RESULTADOS DA PESQUISA");
            System.out.println("Período: " + startDate.format(DATE_FORMATTER) + " até " + endDate.format(DATE_FORMATTER));
            System.out.println("=".repeat(80));
            
            if (accessPoints.isEmpty()) {
                System.out.println("Nenhum Access Point encontrado no período especificado.");
            } else {
                System.out.printf("%-20s %-17s %-10s %-12s %-8s %-10s %-15s %-16s%n",
                        "SSID", "MAC Address", "Qualidade", "Sinal(dBm)", "Canal", "Freq(GHz)", "Segurança", "Data/Hora");
                System.out.println("-".repeat(80));
                
                for (AccessPoint ap : accessPoints) {
                    System.out.printf("%-20s %-17s %-10.1f %-12d %-8d %-10.3f %-15s %-16s%n",
                            truncate(ap.getSsid() != null ? ap.getSsid() : "N/A", 20),
                            ap.getMacAddress() != null ? ap.getMacAddress() : "N/A",
                            ap.getLinkQuality(),
                            ap.getSignalLevel(),
                            ap.getChannel(),
                            ap.getFrequency(),
                            truncate(ap.getSecurityVersion() != null ? ap.getSecurityVersion() : "N/A", 15),
                            ap.getLastBeaconTime() != null ? 
                                ap.getLastBeaconTime().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) : "N/A"
                    );
                }
                
                System.out.println("-".repeat(80));
                System.out.println("Total: " + accessPoints.size() + " registros encontrados");
            }
            
        } catch (DateTimeParseException e) {
            System.err.println("Formato de data inválido! Use: dd/MM/yyyy HH:mm");
        } catch (Exception e) {
            System.err.println("Erro na pesquisa: " + e.getMessage());
        }
        
        System.out.println("\nPressione ENTER para continuar...");
        scanner.nextLine();
    }
    
    private static String truncate(String str, int maxLength) {
        if (str == null) return "N/A";
        return str.length() > maxLength ? str.substring(0, maxLength - 3) + "..." : str;
    }
}