package routing.entity.result;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

// complete set of routes/cars for given task
public class Result {

    private String methodUsed;
    private List<Itinerary> itineraries;
    private double distanceTotal;
    private double timeTotal;
    private int itineraryQty;


}
