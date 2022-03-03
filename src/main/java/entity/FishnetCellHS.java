package entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLHStoreType;
import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.locationtech.jts.geom.MultiPolygon;

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
@Table(name = "fishnet_ver")

//@TypeDef(name = "hstore", typeClass = PostgreSQLHStoreType.class)
@TypeDef(name = "json", typeClass = JsonType.class)

public class FishnetCellHS implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "cell_id", nullable = false)
    private int id;

    @Column(name = "geom",columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;

    @Column(name = "cnt_home")
    private int home;
    @Column(name = "cnt_work")
    private int work;

    @Type(type = "json")
    @Column(name = "min_dist_to_stop",columnDefinition = "json")
    private Map<Integer, Double> minStopDist = new HashMap<>();

    @Type(type = "json")
    @Column(name = "min_dist_to_metro",columnDefinition = "json")
    private Map<Integer, Double> metroSimple = new HashMap<>();

    @Type(type = "json")
    @Column(name = "min_dist_to_metro_full",columnDefinition = "json")
    private Map<Integer, Double> metroFull = new HashMap<>();

    @Type(type = "json")
    @Column(name = "min_dist_to_metro_car",columnDefinition = "json")
    private Map<Integer, Double> metroCar = new HashMap<>();

    @Type(type = "json")
    @Column(name = "nearest_metro",columnDefinition = "json")
    private Map<Integer, String> nearestMetro = new HashMap<>();

    @Type(type = "json")
    @Column(name = "nearest_metro_car",columnDefinition = "json")
    private Map<Integer, String> nearestMetroCar = new HashMap<>();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        FishnetCellHS that = (FishnetCellHS) o;
        return id != 0 && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
