package routing.entity.eval;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class V01params {
    int siteRadius;
    int minTerminalGap;
    int removeWithLessUnique;
    int minDistanceBetweenStops;
    boolean log;
    boolean onePair;
    boolean onlyMetro;
}
