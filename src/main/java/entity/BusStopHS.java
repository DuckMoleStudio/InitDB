package entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLHStoreType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.locationtech.jts.geom.Point;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "bus_stops_ver")
//@TypeDef(name = "hstore", typeClass = PostgreSQLHStoreType.class)
@TypeDef(name = "json", typeClass = JsonType.class)

public class BusStopHS implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "stop_id", nullable = false)
    private int id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "geom",columnDefinition = "geometry(Point,4326)")
    private Point geom;


    @Column(name = "min_dist_to_metro")
    private double minMetroDist;

    @Type(type = "json")
    @Column(name = "min_trip_to_metro",columnDefinition = "json")
    private Map<Integer, Double> tripSimple = new HashMap<>();

    @Type(type = "json")
    @Column(name = "min_trip_to_metro_full",columnDefinition = "json")
    private Map<Integer, Double> tripFull = new HashMap<>();

    @Type(type = "json")
    @Column(name = "nearest_metro",columnDefinition = "json")
    private Map<Integer, String> nearestMetro = new HashMap<>();

    @Type(type = "json")
    @Column(name = "is_active",columnDefinition = "json")
    private Map<Integer, Boolean> active = new HashMap<>();

    @Column(name = "is_terminal")
    private boolean terminal;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BusStopHS busStop = (BusStopHS) o;
        return id != 0 && Objects.equals(id, busStop.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
