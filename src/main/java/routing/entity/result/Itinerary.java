package routing.entity.result;

import lombok.*;
import routing.entity.WayPoint;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode
@With

// Single route for a single car
public class Itinerary {

    private int id;
    private int dir;
    private  int route;
    private String name;
    private List<WayPoint> wayPointList = new ArrayList<>();
    private List<LocalTime> arrivals = new ArrayList<>();
    private LocalTime timeStart, timeEnd;
    private double distance=0;
    private double time=0;

    public WayPoint getFirst()
    {
        return this.wayPointList.get(0);
    }

    public  WayPoint getLast()
    {
        return this.wayPointList.get(this.wayPointList.size()-1);
    }
}

