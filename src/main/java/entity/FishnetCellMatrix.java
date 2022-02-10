package entity;

import com.vladmihalcea.hibernate.type.basic.PostgreSQLHStoreType;
import lombok.*;
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
@Table(name = "fishnet_matrix")
@TypeDef(name = "hstore", typeClass = PostgreSQLHStoreType.class)
public class FishnetCellMatrix implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "cell_id", nullable = false)
    //@GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    @Column(name = "depart")
    private int depart;

    @Column(name = "geom",columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;

    @Type(type = "hstore")
    @Column(name = "dest_cells",columnDefinition = "hstore")
    private Map<Integer, Integer> destCells = new HashMap<>();


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        FishnetCellMatrix that = (FishnetCellMatrix) o;
        return id != 0 && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}