package routing.entity.eval;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class AlgoParams {
    int minDistance; // V00 specific
    int maxDistance; // V00 specific
    int siteRadius; // V00, V01 -- different context
    int addNoLessNewStops; // V00 specific
    int removeWithLessUnique; // V00, V01
    int minDistanceBetweenStops=100; // common output cleansing
    boolean log=false; // common control
    boolean onePair; // V00, V01
    int minTerminalGap; // V01 specific
    boolean onlyMetro; // common input filter
    boolean to, from; // V90 specific
    int capacity;
    int iterations; // these 2 for jsprit algo
    int maxDetour; // for FillLink
    int popToDiscard; // for pop-specific discarding
    boolean pop; // discard by pop
    double reverseDetour; // for reverse route generation
}
