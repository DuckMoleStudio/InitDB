package entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.locationtech.jts.geom.MultiPolygon;

import javax.persistence.*;
import java.util.List;

@Getter
@Setter
@ToString
@RequiredArgsConstructor

@Entity
@Table(name = "fishnet_static")
public class FishnetStatic {
    @Id
    @Column(name = "cell_id", nullable = false)
    private int id;

    @JsonIgnore
    @OneToMany(mappedBy = "id.fishnetStatic")
    private List<FishnetData> fishnetDataList;

    @Column(name = "geom",columnDefinition = "geometry(MultiPolygon,4326)")
    private MultiPolygon geom;

    @Column(name = "cnt_home")
    private int home;
    @Column(name = "cnt_work")
    private int work;
}
