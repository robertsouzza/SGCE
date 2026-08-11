package com.campanha.eleitores.infrastructure.adapter.out.persistence;

import com.campanha.eleitores.domain.Ponto;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

/** Helpers de conversão entre {@link Ponto} do domínio e {@link Point} do JTS. */
public final class GeoFactory {

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private GeoFactory() {}

    public static Point toJts(Ponto p) {
        if (p == null) return null;
        Point point = FACTORY.createPoint(new Coordinate(p.longitude(), p.latitude()));
        point.setSRID(4326);
        return point;
    }

    public static Ponto toDomain(Point p) {
        if (p == null) return null;
        return new Ponto(p.getX(), p.getY());
    }
}
