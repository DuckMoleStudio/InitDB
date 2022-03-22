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
@EqualsAndHashCode

//

@Entity
@Table(name = "admstrip")
public class Admstrip implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "gid", nullable = false)
    private int id;

    @Column(name = "geom",columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;

    @Column(name = "fid")
    private int fid;
}