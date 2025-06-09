package br.unitins.model;

import java.time.LocalDateTime;

/**
 * Classe que representa um Access Point Wi-Fi com todas as informações relevantes
 */
public class AccessPoint {
    private String ssid;                    // Identificador de Conjunto de Serviços
    private String macAddress;              // Endereço MAC do AP (Hexadecimal)
    private double linkQuality;             // Qualidade do Link (%)
    private int signalLevel;                // Nível de Sinal (dBm)
    private int channel;                    // Canal utilizado
    private double frequency;               // Frequência (GHz)
    private LocalDateTime lastBeaconTime;   // Tempo do último frame de sincronismo
    private int beaconInterval;             // Intervalo beacon (ms)
    private String securityVersion;         // Versão segurança Wi-Fi

    public AccessPoint() {}

    public AccessPoint(String ssid, String macAddress, double linkQuality, 
                      int signalLevel, int channel, double frequency,
                      LocalDateTime lastBeaconTime, int beaconInterval, 
                      String securityVersion) {
        this.ssid = ssid;
        this.macAddress = macAddress;
        this.linkQuality = linkQuality;
        this.signalLevel = signalLevel;
        this.channel = channel;
        this.frequency = frequency;
        this.lastBeaconTime = lastBeaconTime;
        this.beaconInterval = beaconInterval;
        this.securityVersion = securityVersion;
    }

    // Getters e Setters
    public String getSsid() { return ssid; }
    public void setSsid(String ssid) { this.ssid = ssid; }

    public String getMacAddress() { return macAddress; }
    public void setMacAddress(String macAddress) { this.macAddress = macAddress; }

    public double getLinkQuality() { return linkQuality; }
    public void setLinkQuality(double linkQuality) { this.linkQuality = linkQuality; }

    public int getSignalLevel() { return signalLevel; }
    public void setSignalLevel(int signalLevel) { this.signalLevel = signalLevel; }

    public int getChannel() { return channel; }
    public void setChannel(int channel) { this.channel = channel; }

    public double getFrequency() { return frequency; }
    public void setFrequency(double frequency) { this.frequency = frequency; }

    public LocalDateTime getLastBeaconTime() { return lastBeaconTime; }
    public void setLastBeaconTime(LocalDateTime lastBeaconTime) { this.lastBeaconTime = lastBeaconTime; }

    public int getBeaconInterval() { return beaconInterval; }
    public void setBeaconInterval(int beaconInterval) { this.beaconInterval = beaconInterval; }

    public String getSecurityVersion() { return securityVersion; }
    public void setSecurityVersion(String securityVersion) { this.securityVersion = securityVersion; }

    @Override
    public String toString() {
        return String.format("""
            ========================================
            SSID/ESSID: %s
            Endereço MAC: %s
            Qualidade do Link: %.1f%%
            Nível de Sinal: %d dBm
            Canal: %d
            Frequência: %.3f GHz
            Último Beacon: %s
            Intervalo Beacon: %d ms
            Segurança Wi-Fi: %s
            ========================================
            """, 
            ssid != null ? ssid : "N/A",
            macAddress != null ? macAddress : "N/A",
            linkQuality,
            signalLevel,
            channel,
            frequency,
            lastBeaconTime != null ? lastBeaconTime.toString() : "N/A",
            beaconInterval,
            securityVersion != null ? securityVersion : "N/A"
        );
    }
} 