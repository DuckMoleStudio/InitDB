package routing.entity.result;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

// Lists of forward & reverse trips for a route

public class TrackList {
    List<Itinerary> forward = new ArrayList<>();
    List<Itinerary> reverse = new ArrayList<>();
}
