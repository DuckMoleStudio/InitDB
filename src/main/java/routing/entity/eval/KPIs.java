package routing.entity.eval;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class KPIs {
    double cellToStop;
    double cellToMetroSimple;
    double cellToMetroFull;
    double totalDistance;
    int routeCount;
    int stopCount;
}
