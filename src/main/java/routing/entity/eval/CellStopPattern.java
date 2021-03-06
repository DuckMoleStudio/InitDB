package routing.entity.eval;

import entity.FishnetCellVer;
import lombok.*;
import routing.entity.WayPoint;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString

public class CellStopPattern {
    List<FishnetCellVer> cells = new ArrayList<>();
    Map<FishnetCellVer,List<Object[]>> stopsNearest = new HashMap<>();
    Map<FishnetCellVer, List<WayPoint>> stopsForCell = new HashMap<>();
    Map<WayPoint,List<FishnetCellVer>> cellsForStop = new HashMap<>();
}
