package br.unitins.service.impl;

import br.unitins.model.AccessPoint;
import br.unitins.service.WiFiDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementação do detector Wi-Fi para sistemas Linux
 * Utiliza o comando iwlist para obter informações dos Access Points
 */
public class LinuxWiFiDetector implements WiFiDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(LinuxWiFiDetector.class);
    
    @Override
    public List<AccessPoint> scanAccessPoints() throws Exception {
        List<AccessPoint> accessPoints = new ArrayList<>();
        
        try {
            // Primeiro tenta encontrar uma interface Wi-Fi ativa
            String wifiInterface = findWiFiInterface();
            if (wifiInterface == null) {
                throw new Exception("Nenhuma interface Wi-Fi encontrada");
            }
            
            // Executa o comando iwlist para escanear redes Wi-Fi
            ProcessBuilder processBuilder = new ProcessBuilder(
                "iwlist", wifiInterface, "scan"
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                AccessPoint currentAP = null;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    // Nova célula (Access Point)
                    if (line.contains("Cell") && line.contains("Address:")) {
                        if (currentAP != null) {
                            accessPoints.add(currentAP);
                        }
                        currentAP = new AccessPoint();
                        // Extrai o endereço MAC
                        String mac = extractMacAddress(line);
                        currentAP.setMacAddress(mac);
                        currentAP.setLastBeaconTime(LocalDateTime.now());
                    }
                    
                    if (currentAP != null) {
                        parseIwlistLine(line, currentAP);
                    }
                }
                
                if (currentAP != null) {
                    accessPoints.add(currentAP);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0 && accessPoints.isEmpty()) {
                throw new Exception("Comando iwlist falhou com código: " + exitCode);
            }
            
        } catch (Exception e) {
            logger.error("Erro ao escanear Access Points: {}", e.getMessage());
            throw new Exception("Falha ao escanear redes Wi-Fi: " + e.getMessage(), e);
        }
        
        return accessPoints;
    }
    
    private String findWiFiInterface() throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder("iwconfig");
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("IEEE 802.11") || line.contains("ESSID")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length > 0) {
                        return parts[0];
                    }
                }
            }
        }
        
        // Se iwconfig não funcionar, tenta listar interfaces de rede
        processBuilder = new ProcessBuilder("ls", "/sys/class/net/");
        process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("wl") || line.startsWith("wlan")) {
                    return line.trim();
                }
            }
        }
        
        return "wlan0"; // Fallback padrão
    }
    
    private String extractMacAddress(String line) {
        Pattern pattern = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return "N/A";
    }
    
    private void parseIwlistLine(String line, AccessPoint ap) {
        try {
            if (line.contains("ESSID:")) {
                String ssid = extractQuotedValue(line);
                if (ssid != null && !ssid.isEmpty()) {
                    ap.setSsid(ssid);
                }
            } else if (line.contains("Quality=")) {
                parseQualityAndSignal(line, ap);
            } else if (line.contains("Channel:")) {
                int channel = extractChannel(line);
                ap.setChannel(channel);
                if (channel > 0) {
                    ap.setFrequency(calculateFrequencyFromChannel(channel));
                }
            } else if (line.contains("Frequency:")) {
                double frequency = extractFrequency(line);
                ap.setFrequency(frequency);
            } else if (line.contains("Encryption key:")) {
                String encryption = line.contains("on") ? "WEP" : "Open";
                ap.setSecurityVersion(encryption);
            } else if (line.contains("IE: IEEE 802.11i/WPA2")) {
                ap.setSecurityVersion("WPA2");
            } else if (line.contains("IE: WPA")) {
                ap.setSecurityVersion("WPA");
            } else if (line.contains("Last beacon:")) {
                // Extrai informações do último beacon se disponível
                ap.setBeaconInterval(100); // Valor padrão
            }
            
            // Define valores padrão se não foram definidos
            if (ap.getBeaconInterval() == 0) {
                ap.setBeaconInterval(100);
            }
            if (ap.getSecurityVersion() == null) {
                ap.setSecurityVersion("Unknown");
            }
            
        } catch (Exception e) {
            logger.warn("Erro ao processar linha: {}", line);
        }
    }
    
    private String extractQuotedValue(String line) {
        Pattern pattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    private void parseQualityAndSignal(String line, AccessPoint ap) {
        // Exemplo: Quality=70/70  Signal level=-40 dBm
        Pattern qualityPattern = Pattern.compile("Quality=(\\d+)/(\\d+)");
        Matcher qualityMatcher = qualityPattern.matcher(line);
        if (qualityMatcher.find()) {
            int quality = Integer.parseInt(qualityMatcher.group(1));
            int maxQuality = Integer.parseInt(qualityMatcher.group(2));
            double qualityPercent = (double) quality / maxQuality * 100.0;
            ap.setLinkQuality(qualityPercent);
        }
        
        Pattern signalPattern = Pattern.compile("Signal level=(-?\\d+)");
        Matcher signalMatcher = signalPattern.matcher(line);
        if (signalMatcher.find()) {
            int signalLevel = Integer.parseInt(signalMatcher.group(1));
            ap.setSignalLevel(signalLevel);
            
            // Se não conseguiu qualidade, calcula baseado no sinal
            if (ap.getLinkQuality() == 0.0) {
                ap.setLinkQuality(calculateLinkQuality(signalLevel));
            }
        }
    }
    
    private int extractChannel(String line) {
        Pattern pattern = Pattern.compile("Channel:(\\d+)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
    
    private double extractFrequency(String line) {
        Pattern pattern = Pattern.compile("Frequency:(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }
    
    private double calculateFrequencyFromChannel(int channel) {
        if (channel >= 1 && channel <= 14) {
            // 2.4 GHz band
            return 2.412 + (channel - 1) * 0.005;
        } else if (channel >= 36 && channel <= 165) {
            // 5 GHz band
            return 5.0 + (channel * 0.005);
        }
        return 0.0;
    }
    
    private double calculateLinkQuality(int signalLevel) {
        // Converte dBm para porcentagem (aproximação)
        if (signalLevel >= -30) return 100.0;
        if (signalLevel >= -67) return 100.0 - ((67 + signalLevel) * 1.5);
        if (signalLevel >= -70) return 50.0 - ((70 + signalLevel) * 3.0);
        if (signalLevel >= -80) return 20.0 - ((80 + signalLevel) * 2.0);
        if (signalLevel >= -90) return 5.0 - ((90 + signalLevel) * 0.5);
        return 0.0;
    }
    
    @Override
    public boolean isSupported() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("linux");
    }
    
    @Override
    public String getDetectorName() {
        return "Linux WiFi Detector (iwlist)";
    }
} 