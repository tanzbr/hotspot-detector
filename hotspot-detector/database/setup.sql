-- Script de configuração do banco de dados MariaDB para Hotspot Detector
-- MariaDB Server 11.4.5

-- Criar banco de dados
CREATE DATABASE IF NOT EXISTS hotspot_detector
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Usar o banco de dados
USE hotspot_detector;

-- Criar usuário para a aplicação
CREATE USER IF NOT EXISTS 'hotspot_user'@'localhost' IDENTIFIED BY 'hotspot_password';
CREATE USER IF NOT EXISTS 'hotspot_user'@'%' IDENTIFIED BY 'hotspot_password';

-- Conceder privilégios
GRANT ALL PRIVILEGES ON hotspot_detector.* TO 'hotspot_user'@'localhost';
GRANT ALL PRIVILEGES ON hotspot_detector.* TO 'hotspot_user'@'%';
FLUSH PRIVILEGES;

-- Criar tabela access_points (será criada automaticamente pelo Hibernate, mas aqui está a estrutura)
-- Esta tabela segue o formato da imagem fornecida
CREATE TABLE IF NOT EXISTS access_points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ssid VARCHAR(100) COMMENT 'Nome da Rede (SSID)',
    mac_address VARCHAR(17) NOT NULL COMMENT 'MAC AP',
    link_quality DECIMAL(5,2) COMMENT 'Qualidade do Link (%)',
    signal_level INT COMMENT 'Nível de Sinal (dBm)',
    channel INT COMMENT 'Canal (Primary Channel)',
    frequency DECIMAL(6,3) COMMENT 'Frequência (GHz)',
    last_beacon INT COMMENT 'Last beacon (ms)',
    beacon_interval INT COMMENT 'Beacon Interval (TUs)',
    wps_wpa_version VARCHAR(50) COMMENT 'WPS/WPA Version',
    scan_time DATETIME NOT NULL COMMENT 'Timestamp do escaneamento',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME ON UPDATE CURRENT_TIMESTAMP,
    
    -- Índices para otimizar consultas
    INDEX idx_ssid (ssid),
    INDEX idx_mac_address (mac_address),
    INDEX idx_scan_time (scan_time),
    INDEX idx_link_quality (link_quality),
    INDEX idx_signal_level (signal_level),
    INDEX idx_channel (channel),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci
  COMMENT='Tabela para armazenar dados dos Access Points Wi-Fi detectados';

-- Criar view para dados mais recentes por MAC Address
CREATE OR REPLACE VIEW latest_access_points AS
SELECT ap.*
FROM access_points ap
INNER JOIN (
    SELECT mac_address, MAX(scan_time) as max_scan_time
    FROM access_points
    GROUP BY mac_address
) latest ON ap.mac_address = latest.mac_address 
         AND ap.scan_time = latest.max_scan_time
ORDER BY ap.link_quality DESC, ap.signal_level DESC;

-- Criar view para estatísticas
CREATE OR REPLACE VIEW access_points_stats AS
SELECT 
    COUNT(*) as total_records,
    COUNT(DISTINCT mac_address) as unique_access_points,
    AVG(link_quality) as avg_link_quality,
    MAX(scan_time) as last_scan_time,
    MIN(scan_time) as first_scan_time,
    COUNT(DISTINCT DATE(scan_time)) as scan_days
FROM access_points;

-- Criar procedure para limpeza de dados antigos
DELIMITER //
CREATE OR REPLACE PROCEDURE CleanupOldData(IN retention_days INT)
BEGIN
    DECLARE deleted_count INT DEFAULT 0;
    DECLARE cutoff_date DATETIME;
    
    SET cutoff_date = DATE_SUB(NOW(), INTERVAL retention_days DAY);
    
    DELETE FROM access_points 
    WHERE scan_time < cutoff_date;
    
    SET deleted_count = ROW_COUNT();
    
    SELECT CONCAT('Removidos ', deleted_count, ' registros anteriores a ', cutoff_date) as result;
END //
DELIMITER ;

-- Criar procedure para obter estatísticas detalhadas
DELIMITER //
CREATE OR REPLACE PROCEDURE GetDetailedStats()
BEGIN
    SELECT 
        'Estatísticas Gerais' as category,
        total_records as value,
        'Total de registros' as description
    FROM access_points_stats
    
    UNION ALL
    
    SELECT 
        'Estatísticas Gerais' as category,
        unique_access_points as value,
        'Access Points únicos' as description
    FROM access_points_stats
    
    UNION ALL
    
    SELECT 
        'Qualidade' as category,
        ROUND(avg_link_quality, 2) as value,
        'Qualidade média (%)' as description
    FROM access_points_stats
    
    UNION ALL
    
    SELECT 
        'Canais' as category,
        COUNT(*) as value,
        CONCAT('Canal ', channel) as description
    FROM latest_access_points
    WHERE channel IS NOT NULL
    GROUP BY channel
    ORDER BY channel
    
    UNION ALL
    
    SELECT 
        'Segurança' as category,
        COUNT(*) as value,
        CONCAT('Tipo: ', COALESCE(wps_wpa_version, 'Desconhecido')) as description
    FROM latest_access_points
    GROUP BY wps_wpa_version
    ORDER BY COUNT(*) DESC;
END //
DELIMITER ;

-- Inserir dados de exemplo (opcional)
-- INSERT INTO access_points (ssid, mac_address, link_quality, signal_level, channel, frequency, last_beacon, beacon_interval, wps_wpa_version, scan_time)
-- VALUES 
-- ('Unitins', '4A:5A:B6:29:F1:F2', 80.0, -59, 11, 2.462, 592, 100, '1.0', NOW()),
-- ('WiFi_Exemplo', 'AA:BB:CC:DD:EE:FF', 65.0, -70, 6, 2.437, 400, 100, 'WPA2', NOW());

-- Mostrar estrutura criada
SHOW TABLES;
DESCRIBE access_points;

-- Mostrar views criadas
SHOW FULL TABLES WHERE Table_type = 'VIEW';

-- Mostrar procedures criadas
SHOW PROCEDURE STATUS WHERE Db = 'hotspot_detector';

SELECT 'Banco de dados hotspot_detector configurado com sucesso!' as status; 