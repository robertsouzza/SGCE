package com.campanha.eleitores.domain;

/**
 * Ponto geográfico WGS84 no domínio — desacopla o modelo de negócio
 * do JTS/Hibernate Spatial (que ficam confinados na infrastructure).
 */
public record Ponto(double longitude, double latitude) {
    public Ponto {
        if (longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude fora de faixa: " + longitude);
        }
        if (latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude fora de faixa: " + latitude);
        }
    }
}
