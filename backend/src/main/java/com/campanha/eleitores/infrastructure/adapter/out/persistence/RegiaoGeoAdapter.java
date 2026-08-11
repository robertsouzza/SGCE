package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import com.campanha.eleitores.application.port.out.RegiaoGeoPort;
import com.campanha.eleitores.domain.NivelRegiao;
import com.campanha.eleitores.domain.Ponto;
import com.campanha.eleitores.domain.RegiaoEleitoral;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Consultas geoespaciais via SQL nativo (PostGIS ST_Contains). Retorna a
 * região mais fina (nível mais alto: BAIRRO_ZONA > MUNICIPIO > ESTADO > PAIS)
 * que contém um ponto — útil para calcular a região do eleitor a partir da
 * sua geolocalização.
 */
@Component
@RequiredArgsConstructor
public class RegiaoGeoAdapter implements RegiaoGeoPort {

    @PersistenceContext
    private final EntityManager em;

    @Override
    @SuppressWarnings("unchecked")
    public Optional<RegiaoEleitoral> encontrarRegiaoMaisFinaContendo(Ponto ponto) {
        if (ponto == null) return Optional.empty();
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id, nivel, regiao_pai_id, codigo_ibge, nome_regiao " +
                        "FROM regioes_eleitorais " +
                        "WHERE ST_Contains(geometria, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)) " +
                        "ORDER BY CASE nivel WHEN 'BAIRRO_ZONA' THEN 4 WHEN 'MUNICIPIO' THEN 3 " +
                        "  WHEN 'ESTADO' THEN 2 WHEN 'PAIS' THEN 1 END DESC " +
                        "LIMIT 1")
                .setParameter("lon", ponto.longitude())
                .setParameter("lat", ponto.latitude())
                .getResultList();
        if (rows.isEmpty()) return Optional.empty();
        Object[] r = rows.get(0);
        return Optional.of(new RegiaoEleitoral(
                ((Number) r[0]).longValue(),
                NivelRegiao.valueOf((String) r[1]),
                r[2] == null ? null : ((Number) r[2]).longValue(),
                (String) r[3],
                (String) r[4]));
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<RegiaoEleitoral> listarTodas() {
        List<Object[]> rows = em.createNativeQuery(
                "SELECT id, nivel, regiao_pai_id, codigo_ibge, nome_regiao FROM regioes_eleitorais ORDER BY id")
                .getResultList();
        return rows.stream()
                .map(r -> new RegiaoEleitoral(
                        ((Number) r[0]).longValue(),
                        NivelRegiao.valueOf((String) r[1]),
                        r[2] == null ? null : ((Number) r[2]).longValue(),
                        (String) r[3],
                        (String) r[4]))
                .toList();
    }
}
