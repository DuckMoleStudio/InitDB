package routing.entity.storage;

import lombok.*;
import routing.entity.WayPoint;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode

// internal
public class Hop {
    WayPoint from,to;
}
