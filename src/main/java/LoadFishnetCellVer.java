import entity.FishnetCell;
import entity.FishnetCellVer;

import java.util.List;

import static service.FishnetCellVerService.updateFishnetCellVer;
import static service.FishnetCellService.listFishnetCells;

public class LoadFishnetCellVer {
    public static void main(String[] args) {

        List<FishnetCell> oldFishnet = listFishnetCells();

        for(FishnetCell cell: oldFishnet)
        {
            FishnetCellVer newCell = new FishnetCellVer();
            newCell.setId(Integer.parseInt(cell.getId()));
            newCell.setHome(cell.getHome());
            newCell.setWork(cell.getWork());
            newCell.setGeom(cell.getGeom());

            updateFishnetCellVer(newCell);
        }
    }
}
