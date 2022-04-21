package routing.entity.eval;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class V00params {
    int minDistance;
    int maxDistance;
    int siteRadius;
    int addNoLessNewStops;
    int removeWithLessUnique;
    int minDistanceBetweenStops;
    boolean log;
}
