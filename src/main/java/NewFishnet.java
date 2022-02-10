import entity.FishnetCell;
import entity.FishnetCell2;

import java.util.List;

import static service.FishnetCell2Service.addFishnetCell2;
import static service.FishnetCellService.listFishnetCells;

public class NewFishnet {
    public static void main(String[] args) {

        List<FishnetCell> oldFishnet = listFishnetCells();

        for(FishnetCell cell: oldFishnet)
        {
            FishnetCell2 newCell = new FishnetCell2();
            newCell.setId(Integer.parseInt(cell.getId()));
            newCell.setHome(cell.getHome());
            newCell.setWork(cell.getWork());
            newCell.setGeom(cell.getGeom());
            newCell.setMinStopDist(cell.getMinStopDist());

            addFishnetCell2(newCell);
        }
    }
}
