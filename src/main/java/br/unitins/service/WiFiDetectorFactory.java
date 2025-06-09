package br.unitins.service;

import br.unitins.service.impl.LinuxWiFiDetector;
import br.unitins.service.impl.WindowsWiFiDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory para criar detectores Wi-Fi apropriados para cada sistema operacional
 */
public class WiFiDetectorFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(WiFiDetectorFactory.class);
    
    /**
     * Cria um detector Wi-Fi apropriado para o sistema operacional atual
     * @return Detector Wi-Fi compatível ou null se nenhum for encontrado
     */
    public static WiFiDetector createDetector() {
        List<WiFiDetector> detectors = getAllDetectors();
        
        for (WiFiDetector detector : detectors) {
            if (detector.isSupported()) {
                logger.info("Usando detector: {}", detector.getDetectorName());
                return detector;
            }
        }
        
        logger.warn("Nenhum detector Wi-Fi compatível encontrado para este sistema");
        return null;
    }
    
    /**
     * Retorna todos os detectores disponíveis
     * @return Lista de todos os detectores
     */
    public static List<WiFiDetector> getAllDetectors() {
        List<WiFiDetector> detectors = new ArrayList<>();
        detectors.add(new WindowsWiFiDetector());
        detectors.add(new LinuxWiFiDetector());
        return detectors;
    }
    
    /**
     * Retorna informações sobre o sistema operacional atual
     * @return String com informações do SO
     */
    public static String getSystemInfo() {
        return String.format("SO: %s %s (%s)", 
            System.getProperty("os.name"),
            System.getProperty("os.version"),
            System.getProperty("os.arch")
        );
    }
} 