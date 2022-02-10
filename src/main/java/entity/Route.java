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
@Table(name = "routes")
public class Route implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "track_id", nullable = false)
    //@GeneratedValue(strategy = GenerationType.AUTO)
    private int id;



    @Column(name = "short_name")
    private String shortName;
    @Column(name = "long_name")
    private String longName;

    @Type(type = "string-array")
    @Column(name = "stops")
    private String[] stops;

    @Type(type = "double-array")
    @Column(name = "distances")
    private double[] distances;


    @Column(name = "geom",columnDefinition = "geometry(MultiLineString,4326)")
    private MultiLineString geomML;

    @Column(name = "route_id")
    private String rid;
    @Column(name = "route_dir")
    private String dir;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Route route = (Route) o;
        return id != 0 && Objects.equals(id, route.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
