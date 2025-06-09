package br.unitins.service;

import br.unitins.model.AccessPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serviço principal para detecção de Access Points Wi-Fi
 */
public class HotspotDetectorService {
    
    private static final Logger logger = LoggerFactory.getLogger(HotspotDetectorService.class);
    private final WiFiDetector detector;
    
    public HotspotDetectorService() {
        this.detector = WiFiDetectorFactory.createDetector();
        if (this.detector == null) {
            throw new RuntimeException("Nenhum detector Wi-Fi compatível encontrado");
        }
    }
    
    /**
     * Escaneia Access Points disponíveis na região
     * @return Lista de Access Points encontrados
     * @throws Exception se houver erro durante o escaneamento
     */
    public List<AccessPoint> scanAccessPoints() throws Exception {
        logger.info("Iniciando escaneamento de Access Points...");
        
        List<AccessPoint> accessPoints = detector.scanAccessPoints();
        
        logger.info("Escaneamento concluído. {} Access Points encontrados", accessPoints.size());
        
        return accessPoints;
    }
    
    /**
     * Escaneia e filtra Access Points por SSID
     * @param ssidFilter Filtro para SSID (case-insensitive)
     * @return Lista filtrada de Access Points
     * @throws Exception se houver erro durante o escaneamento
     */
    public List<AccessPoint> scanAccessPointsBySSID(String ssidFilter) throws Exception {
        List<AccessPoint> allAccessPoints = scanAccessPoints();
        
        return allAccessPoints.stream()
                .filter(ap -> ap.getSsid() != null && 
                             ap.getSsid().toLowerCase().contains(ssidFilter.toLowerCase()))
                .collect(Collectors.toList());
    }
    
    /**
     * Escaneia e filtra Access Points por qualidade mínima do sinal
     * @param minQuality Qualidade mínima do link (0-100%)
     * @return Lista filtrada de Access Points
     * @throws Exception se houver erro durante o escaneamento
     */
    public List<AccessPoint> scanAccessPointsByQuality(double minQuality) throws Exception {
        List<AccessPoint> allAccessPoints = scanAccessPoints();
        
        return allAccessPoints.stream()
                .filter(ap -> ap.getLinkQuality() >= minQuality)
                .collect(Collectors.toList());
    }
    
    /**
     * Escaneia e ordena Access Points por qualidade do sinal (melhor primeiro)
     * @return Lista ordenada de Access Points
     * @throws Exception se houver erro durante o escaneamento
     */
    public List<AccessPoint> scanAccessPointsByBestQuality() throws Exception {
        List<AccessPoint> allAccessPoints = scanAccessPoints();
        
        return allAccessPoints.stream()
                .sorted((ap1, ap2) -> Double.compare(ap2.getLinkQuality(), ap1.getLinkQuality()))
                .collect(Collectors.toList());
    }
    
    /**
     * Retorna informações sobre o detector sendo usado
     * @return Nome do detector
     */
    public String getDetectorInfo() {
        return detector.getDetectorName();
    }
    
    /**
     * Exibe um relatório detalhado dos Access Points
     * @param accessPoints Lista de Access Points para exibir
     */
    public void displayReport(List<AccessPoint> accessPoints) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("RELATÓRIO DE ACCESS POINTS DETECTADOS");
        System.out.println("=".repeat(60));
        System.out.println("Sistema: " + WiFiDetectorFactory.getSystemInfo());
        System.out.println("Detector: " + getDetectorInfo());
        System.out.println("Total de APs encontrados: " + accessPoints.size());
        System.out.println("=".repeat(60));
        
        if (accessPoints.isEmpty()) {
            System.out.println("Nenhum Access Point foi encontrado.");
            System.out.println("Verifique se:");
            System.out.println("- O Wi-Fi está habilitado");
            System.out.println("- Você tem permissões adequadas");
            System.out.println("- Existem redes Wi-Fi na região");
        } else {
            for (int i = 0; i < accessPoints.size(); i++) {
                System.out.println("\n--- ACCESS POINT " + (i + 1) + " ---");
                System.out.println(accessPoints.get(i).toString());
            }
        }
        
        System.out.println("=".repeat(60));
    }
    
    /**
     * Exibe um resumo compacto dos Access Points
     * @param accessPoints Lista de Access Points para exibir
     */
    public void displaySummary(List<AccessPoint> accessPoints) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("RESUMO DOS ACCESS POINTS");
        System.out.println("=".repeat(80));
        System.out.printf("%-20s %-18s %-8s %-10s %-8s %-12s%n", 
                         "SSID", "MAC Address", "Quality", "Signal", "Channel", "Security");
        System.out.println("-".repeat(80));
        
        for (AccessPoint ap : accessPoints) {
            System.out.printf("%-20s %-18s %-8.1f %-10d %-8d %-12s%n",
                truncate(ap.getSsid(), 20),
                truncate(ap.getMacAddress(), 18),
                ap.getLinkQuality(),
                ap.getSignalLevel(),
                ap.getChannel(),
                truncate(ap.getSecurityVersion(), 12)
            );
        }
        
        System.out.println("=".repeat(80));
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) return "N/A";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
} 