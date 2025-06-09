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
 * Implementação do detector Wi-Fi para sistemas Windows
 * Utiliza o comando netsh para obter informações dos Access Points
 */
public class WindowsWiFiDetector implements WiFiDetector {
    
    private static final Logger logger = LoggerFactory.getLogger(WindowsWiFiDetector.class);
    
    @Override
    public List<AccessPoint> scanAccessPoints() throws Exception {
        List<AccessPoint> accessPoints = new ArrayList<>();
        
        try {
            // Executa o comando netsh para escanear redes Wi-Fi
            ProcessBuilder processBuilder = new ProcessBuilder(
                "netsh", "wlan", "show", "profiles"
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // Primeiro, obtém os perfis salvos
            List<String> profiles = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("Perfil de Todos os Usuários") || 
                        line.contains("All User Profile")) {
                        String profileName = extractProfileName(line);
                        if (profileName != null && !profileName.isEmpty()) {
                            profiles.add(profileName);
                        }
                    }
                }
            }
            
            // Agora escaneia redes disponíveis
            processBuilder = new ProcessBuilder(
                "netsh", "wlan", "show", "interfaces"
            );
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                String line;
                AccessPoint currentAP = null;
                
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    
                    if (line.contains("Nome") || line.contains("Name")) {
                        if (currentAP != null) {
                            accessPoints.add(currentAP);
                        }
                        currentAP = new AccessPoint();
                    }
                    
                    if (currentAP != null) {
                        parseNetshLine(line, currentAP);
                    }
                }
                
                if (currentAP != null) {
                    accessPoints.add(currentAP);
                }
            }
            
            // Se não conseguiu informações detalhadas, tenta comando alternativo
            if (accessPoints.isEmpty()) {
                accessPoints = scanWithAlternativeMethod();
            }
            
        } catch (Exception e) {
            logger.error("Erro ao escanear Access Points: {}", e.getMessage());
            throw new Exception("Falha ao escanear redes Wi-Fi: " + e.getMessage(), e);
        }
        
        return accessPoints;
    }
    
    private List<AccessPoint> scanWithAlternativeMethod() throws Exception {
        List<AccessPoint> accessPoints = new ArrayList<>();
        
        ProcessBuilder processBuilder = new ProcessBuilder(
            "netsh", "wlan", "show", "profiles"
        );
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), "UTF-8"))) {
            String line;
            
            while ((line = reader.readLine()) != null) {
                if (line.contains("Perfil de Todos os Usuários") || 
                    line.contains("All User Profile")) {
                    String profileName = extractProfileName(line);
                    if (profileName != null && !profileName.isEmpty()) {
                        AccessPoint ap = createBasicAccessPoint(profileName);
                        accessPoints.add(ap);
                    }
                }
            }
        }
        
        return accessPoints;
    }
    
    private AccessPoint createBasicAccessPoint(String ssid) {
        AccessPoint ap = new AccessPoint();
        ap.setSsid(ssid);
        ap.setMacAddress("N/A");
        ap.setLinkQuality(0.0);
        ap.setSignalLevel(-100);
        ap.setChannel(0);
        ap.setFrequency(0.0);
        ap.setLastBeaconTime(LocalDateTime.now());
        ap.setBeaconInterval(100);
        ap.setSecurityVersion("N/A");
        return ap;
    }
    
    private String extractProfileName(String line) {
        // Extrai o nome do perfil da linha
        String[] parts = line.split(":");
        if (parts.length > 1) {
            return parts[1].trim();
        }
        return null;
    }
    
    private void parseNetshLine(String line, AccessPoint ap) {
        try {
            if (line.contains("SSID") && !line.contains("BSSID")) {
                String ssid = extractValue(line);
                ap.setSsid(ssid);
            } else if (line.contains("BSSID") || line.contains("MAC")) {
                String mac = extractValue(line);
                ap.setMacAddress(mac);
            } else if (line.contains("Sinal") || line.contains("Signal")) {
                String signal = extractValue(line);
                try {
                    // Extrai valor numérico do sinal
                    Pattern pattern = Pattern.compile("-?\\d+");
                    Matcher matcher = pattern.matcher(signal);
                    if (matcher.find()) {
                        ap.setSignalLevel(Integer.parseInt(matcher.group()));
                    }
                } catch (NumberFormatException e) {
                    ap.setSignalLevel(-100);
                }
            } else if (line.contains("Canal") || line.contains("Channel")) {
                String channel = extractValue(line);
                try {
                    ap.setChannel(Integer.parseInt(channel.replaceAll("\\D", "")));
                } catch (NumberFormatException e) {
                    ap.setChannel(0);
                }
            } else if (line.contains("Tipo de rede") || line.contains("Network type")) {
                String security = extractValue(line);
                ap.setSecurityVersion(security);
            }
            
            // Define valores padrão se não foram definidos
            if (ap.getLastBeaconTime() == null) {
                ap.setLastBeaconTime(LocalDateTime.now());
            }
            if (ap.getBeaconInterval() == 0) {
                ap.setBeaconInterval(100);
            }
            if (ap.getFrequency() == 0.0 && ap.getChannel() > 0) {
                ap.setFrequency(calculateFrequencyFromChannel(ap.getChannel()));
            }
            
            // Calcula qualidade do link baseada no nível do sinal
            if (ap.getLinkQuality() == 0.0 && ap.getSignalLevel() != 0) {
                ap.setLinkQuality(calculateLinkQuality(ap.getSignalLevel()));
            }
            
        } catch (Exception e) {
            logger.warn("Erro ao processar linha: {}", line);
        }
    }
    
    private String extractValue(String line) {
        String[] parts = line.split(":");
        if (parts.length > 1) {
            return parts[1].trim();
        }
        return "";
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
        return os.contains("windows");
    }
    
    @Override
    public String getDetectorName() {
        return "Windows WiFi Detector (netsh)";
    }
} 