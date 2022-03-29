package routing.entity.storage;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

public class TimeDistancePair {
    private double time;
    private double distance;
}
