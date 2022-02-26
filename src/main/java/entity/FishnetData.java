package entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

@Getter
@Setter
@ToString
@RequiredArgsConstructor

@Entity
@Table(name = "fishnet_data")
public class FishnetData {
    @JsonIgnore
    @EmbeddedId
    private FishnetVersionKey id;

    @Column(name = "min_dist_to_stop")
    private double minStopDist;
    @Column(name = "min_dist_to_metro")
    private double metroSimple;
    @Column(name = "min_dist_to_metro_full")
    private double metroFull;
    @Column(name = "nearest_metro")
    private String nearestMetro;
    @Column(name = "nearest_metro_car")
    private String nearestMetroCar;
    @Column(name = "min_dist_to_metro_car")
    private double metroCar;
}
