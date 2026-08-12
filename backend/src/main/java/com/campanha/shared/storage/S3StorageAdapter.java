package com.campanha.shared.storage;

import com.campanha.consentimento.application.port.out.AssinaturaStoragePort;
import com.campanha.financeiro.application.port.out.ComprovanteStoragePort;
import com.campanha.shared.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;

/**
 * Adapter único para armazenamento de objetos, implementando as ports que
 * cada módulo declara (ComprovanteStoragePort do financeiro,
 * AssinaturaStoragePort do consentimento na skill 05, etc.).
 *
 * Usa MinIO em dev (docker-compose) e S3-compatível em prod. Decisão D-02b:
 * uma implementação, uma API (AWS SDK v2), zero divergência.
 *
 * Frontend baixa via presigned URL (expira em minutos) — backend nunca faz
 * proxy de binário.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class S3StorageAdapter implements ComprovanteStoragePort, AssinaturaStoragePort {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config config;

    /** Salva um blob. Retorna a chave do objeto no bucket. */
    @Override
    public String save(String key, InputStream content, long contentLength, String contentType) throws IOException {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(config.getBucket())
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
        s3Client.putObject(put, RequestBody.fromInputStream(content, contentLength));
        return key;
    }

    /** Gera URL presigned para GET válida por {@code ttl}. */
    @Override
    public String presignedGetUrl(String key, Duration ttl) {
        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(config.getBucket())
                        .key(key)
                        .build())
                .build();
        return s3Presigner.presignGetObject(presign).url().toString();
    }
}
