package com.campanha.consentimento.application.port.out;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * Port específico do módulo consentimento — armazenamento da assinatura
 * em tela (PNG do canvas). Implementado pelo mesmo S3StorageAdapter que
 * atende ComprovanteStoragePort (financeiro). D-02b: uma implementação
 * para múltiplas ports.
 */
public interface AssinaturaStoragePort {
    String save(String key, InputStream content, long contentLength, String contentType) throws IOException;
    String presignedGetUrl(String key, Duration ttl);
}
