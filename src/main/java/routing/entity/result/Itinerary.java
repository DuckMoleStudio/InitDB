package routing.entity.result;

import lombok.*;
import routing.entity.WayPoint;

import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

// Single route for a single car
public class Itinerary {

    private int id;
    private String name;
    private List<WayPoint> wayPointList;
    private List<LocalTime> arrivals;
    private LocalTime timeStart, timeEnd;
    private double distance;
    private double time;
}