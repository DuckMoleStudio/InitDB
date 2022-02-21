package entity;

import lombok.*;
import org.hibernate.Hibernate;
import org.locationtech.jts.geom.MultiPolygon;

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

//

@Entity
@Table(name = "fishnet")
public class FishnetCell implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "cell_id", nullable = false)
    //@GeneratedValue(strategy = GenerationType.AUTO)
    private String id;

    @Column(name = "geom",columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;

    @Column(name = "cnt_home")
    private int home;
    @Column(name = "cnt_work")
    private int work;

    @Column(name = "min_dist_to_stop")
    private double minStopDist;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        FishnetCell that = (FishnetCell) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}