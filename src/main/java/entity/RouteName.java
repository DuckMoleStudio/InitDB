package entity;

import com.vladmihalcea.hibernate.type.array.DoubleArrayType;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import com.vladmihalcea.hibernate.type.basic.PostgreSQLHStoreType;
import com.vladmihalcea.hibernate.type.json.JsonType;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor

@Entity
@Table(name = "route_names")
//@TypeDef(name = "hstore", typeClass = PostgreSQLHStoreType.class)
@TypeDef(name = "json", typeClass = JsonType.class)
public class RouteName implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "route_id", nullable = false)
    private int id;



    @Column(name = "short_name")
    private String shortName;
    @Column(name = "long_name")
    private String longName;

    @Type(type = "json")
    @Column(name = "trips",columnDefinition = "json")
    private Map<Integer, List<Integer>> trips = new HashMap<>();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        RouteName route = (RouteName) o;
        return id != 0 && Objects.equals(id, route.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
