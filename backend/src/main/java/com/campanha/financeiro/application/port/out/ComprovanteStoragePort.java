package com.campanha.financeiro.application.port.out;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * Contrato para persistência de arquivos de comprovante (nota fiscal, recibo).
 * Implementado por infra via S3StorageAdapter (MinIO em dev, S3 em prod — D-02b).
 */
public interface ComprovanteStoragePort {

    /** Salva o binário e retorna a chave para consulta futura. */
    String save(String suggestedKey, InputStream content, long contentLength, String contentType) throws IOException;

    /** URL presigned para GET; frontend baixa direto do S3. */
    String presignedGetUrl(String key, Duration ttl);
}
