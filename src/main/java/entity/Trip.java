package entity;

import com.vladmihalcea.hibernate.type.array.DoubleArrayType;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;
import org.locationtech.jts.geom.MultiLineString;

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

@TypeDefs({
        @TypeDef(
                name = "string-array",
                typeClass = StringArrayType.class
        ),
        @TypeDef(
                name = "double-array",
                typeClass = DoubleArrayType.class
        )
})

@Entity
@Table(name = "trips")
public class Trip implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "trip_id", nullable = false)
    //@GeneratedValue(strategy = GenerationType.AUTO)
    private int id;



    @Type(type = "string-array")
    @Column(name = "stops")
    private String[] stops;

    @Type(type = "double-array")
    @Column(name = "hops")
    private double[] hops;

    @Column(name = "route_id")
    private String rid;
    @Column(name = "route_dir")
    private String dir;

    @Column(name = "interval")
    private double interval;

    @Column(name = "total_time")
    private double totalTime;
    @Column(name = "total_distance")
    private double totalDistance;

    @Column(name = "geom",columnDefinition = "geometry(MultiLineString,4326)")
    private MultiLineString geomML;



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Trip trip = (Trip) o;
        return id != 0 && Objects.equals(id, trip.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
