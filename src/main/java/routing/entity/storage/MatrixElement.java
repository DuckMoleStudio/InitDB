package routing.entity.storage;

import lombok.*;
import routing.entity.WayPoint;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString

// internal, for returning as a result like "nearest", WP itself & time/distance to it from the given WP
public class MatrixElement {
    private WayPoint wayPoint;
    private double distance;
    private double time;
}
