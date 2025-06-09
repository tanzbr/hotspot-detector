package br.unitins.service;

import br.unitins.model.AccessPoint;
import java.util.List;

/**
 * Interface para detectores de Access Points Wi-Fi
 */
public interface WiFiDetector {
    
    /**
     * Escaneia e retorna uma lista de Access Points disponíveis na região
     * @return Lista de Access Points detectados
     * @throws Exception se houver erro durante o escaneamento
     */
    List<AccessPoint> scanAccessPoints() throws Exception;
    
    /**
     * Verifica se o detector é compatível com o sistema operacional atual
     * @return true se compatível, false caso contrário
     */
    boolean isSupported();
    
    /**
     * Retorna o nome do detector
     * @return Nome do detector
     */
    String getDetectorName();
} 