package br.unitins.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidade JPA para persistir dados dos Access Points no banco MariaDB
 * Baseada na estrutura da tabela: Nome da Rede (SSID), MAC AP, Qualidade do Link, 
 * Nível de Sinal, Canal, Frequência, Last beacon, Beacon Interval, WPS/WPA Version
 */
@Entity
@Table(name = "access_points", indexes = {
    @Index(name = "idx_ssid", columnList = "ssid"),
    @Index(name = "idx_mac_address", columnList = "mac_address"),
    @Index(name = "idx_scan_time", columnList = "scan_time"),
    @Index(name = "idx_link_quality", columnList = "link_quality")
})
public class AccessPointEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "ssid", length = 100)
    private String ssid; // Nome da Rede (SSID)
    
    @Column(name = "mac_address", length = 17, nullable = false)
    private String macAddress; // MAC AP
    
    @Column(name = "link_quality", precision = 5, scale = 2)
    private Double linkQuality; // Qualidade do Link (%)
    
    @Column(name = "signal_level")
    private Integer signalLevel; // Nível de Sinal (dBm)
    
    @Column(name = "channel")
    private Integer channel; // Canal (Primary Channel)
    
    @Column(name = "frequency", precision = 6, scale = 3)
    private Double frequency; // Frequência (GHz)
    
    @Column(name = "last_beacon")
    private Integer lastBeacon; // Last beacon (ms)
    
    @Column(name = "beacon_interval")
    private Integer beaconInterval; // Beacon Interval (TUs)
    
    @Column(name = "wps_wpa_version", length = 50)
    private String wpsWpaVersion; // WPS/WPA Version
    
    @Column(name = "scan_time", nullable = false)
    private LocalDateTime scanTime; // Timestamp do escaneamento
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Construtores
    public AccessPointEntity() {
        this.createdAt = LocalDateTime.now();
        this.scanTime = LocalDateTime.now();
    }
    
    public AccessPointEntity(AccessPoint accessPoint) {
        this();
        this.ssid = accessPoint.getSsid();
        this.macAddress = accessPoint.getMacAddress();
        this.linkQuality = accessPoint.getLinkQuality();
        this.signalLevel = accessPoint.getSignalLevel();
        this.channel = accessPoint.getChannel();
        this.frequency = accessPoint.getFrequency();
        this.beaconInterval = accessPoint.getBeaconInterval();
        this.wpsWpaVersion = accessPoint.getSecurityVersion();
        
        // Converte o último beacon time para milliseconds se disponível
        if (accessPoint.getLastBeaconTime() != null) {
            // Para simplificar, vamos usar o intervalo de beacon como referência
            this.lastBeacon = accessPoint.getBeaconInterval();
        }
    }
    
    // Métodos de callback JPA
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    // Getters e Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getSsid() {
        return ssid;
    }
    
    public void setSsid(String ssid) {
        this.ssid = ssid;
    }
    
    public String getMacAddress() {
        return macAddress;
    }
    
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    
    public Double getLinkQuality() {
        return linkQuality;
    }
    
    public void setLinkQuality(Double linkQuality) {
        this.linkQuality = linkQuality;
    }
    
    public Integer getSignalLevel() {
        return signalLevel;
    }
    
    public void setSignalLevel(Integer signalLevel) {
        this.signalLevel = signalLevel;
    }
    
    public Integer getChannel() {
        return channel;
    }
    
    public void setChannel(Integer channel) {
        this.channel = channel;
    }
    
    public Double getFrequency() {
        return frequency;
    }
    
    public void setFrequency(Double frequency) {
        this.frequency = frequency;
    }
    
    public Integer getLastBeacon() {
        return lastBeacon;
    }
    
    public void setLastBeacon(Integer lastBeacon) {
        this.lastBeacon = lastBeacon;
    }
    
    public Integer getBeaconInterval() {
        return beaconInterval;
    }
    
    public void setBeaconInterval(Integer beaconInterval) {
        this.beaconInterval = beaconInterval;
    }
    
    public String getWpsWpaVersion() {
        return wpsWpaVersion;
    }
    
    public void setWpsWpaVersion(String wpsWpaVersion) {
        this.wpsWpaVersion = wpsWpaVersion;
    }
    
    public LocalDateTime getScanTime() {
        return scanTime;
    }
    
    public void setScanTime(LocalDateTime scanTime) {
        this.scanTime = scanTime;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    // Método para converter para AccessPoint
    public AccessPoint toAccessPoint() {
        AccessPoint ap = new AccessPoint();
        ap.setSsid(this.ssid);
        ap.setMacAddress(this.macAddress);
        ap.setLinkQuality(this.linkQuality != null ? this.linkQuality : 0.0);
        ap.setSignalLevel(this.signalLevel != null ? this.signalLevel : -100);
        ap.setChannel(this.channel != null ? this.channel : 0);
        ap.setFrequency(this.frequency != null ? this.frequency : 0.0);
        ap.setBeaconInterval(this.beaconInterval != null ? this.beaconInterval : 100);
        ap.setSecurityVersion(this.wpsWpaVersion);
        ap.setLastBeaconTime(this.scanTime);
        return ap;
    }
    
    @Override
    public String toString() {
        return String.format("""
            AccessPointEntity{
                id=%d, ssid='%s', macAddress='%s', 
                linkQuality=%.2f%%, signalLevel=%d dBm, 
                channel=%d, frequency=%.3f GHz, 
                wpsWpaVersion='%s', scanTime=%s
            }""", 
            id, ssid, macAddress, linkQuality, signalLevel, 
            channel, frequency, wpsWpaVersion, scanTime
        );
    }
} 