package entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
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
@Table(name = "metro")
public class Metro implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "metro_id", nullable = false)
    //@GeneratedValue(strategy = GenerationType.AUTO)
    private int id;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "station", nullable = false)
    private String station;
    @Column(name = "line", nullable = false)
    private String line;

    @Column(name = "geom",columnDefinition = "geometry(Point,4326)")
    private Point geom;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        Metro metro = (Metro) o;
        return id != 0 && Objects.equals(id, metro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
