package entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import lombok.*;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.locationtech.jts.geom.MultiPolygon;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode

@Entity
@Table(name = "admzone")
@TypeDef(name = "json", typeClass = JsonType.class)
public class Admzone implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "zone_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "geom",columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;

    @Type(type = "json")
    @Column(name = "stops",columnDefinition = "json")
    private Map<Integer, Integer> stops = new HashMap<>();

    @Type(type = "json")
    @Column(name = "routes",columnDefinition = "json")
    private Map<Integer, Integer> trips = new HashMap<>();

    @Type(type = "json")
    @Column(name = "total_length",columnDefinition = "json")
    private Map<Integer, Double> distances = new HashMap<>();
}