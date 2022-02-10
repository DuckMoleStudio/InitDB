package entity;

import lombok.*;
import org.hibernate.Hibernate;
import org.locationtech.jts.geom.Point;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "bus_stops")
public class BusStop implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "stop_id", nullable = false)
    //@GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "geom",columnDefinition = "geometry(Point,4326)")
    private Point geom;

    @Column(name = "min_dist_to_metro")
    private double minMetroDist;

    @Column(name = "min_trip_to_metro")
    private double tripSimple;
    @Column(name = "min_trip_to_metro_full")
    private double tripFull;

    @Column(name = "nearest_metro")
    private String nearestMetro;

    @Column(name = "is_active")
    private boolean active;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        BusStop busStop = (BusStop) o;
        return id != 0 && Objects.equals(id, busStop.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
