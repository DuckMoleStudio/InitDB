import entity.FishnetCell;
import entity.FishnetStatic;

import java.util.List;

import static service.FishnetCellService.listFishnetCells;
import static service.FishnetStaticService.addFishnetStatic;

public class LoadFishnetStatic {
    public static void main(String[] args) {

        List<FishnetCell> oldFishnet = listFishnetCells();

        for(FishnetCell cell: oldFishnet)
        {
            FishnetStatic newCell = new FishnetStatic();
            newCell.setId(Integer.parseInt(cell.getId()));
            newCell.setHome(cell.getHome());
            newCell.setWork(cell.getWork());
            newCell.setGeom(cell.getGeom());

            addFishnetStatic(newCell);
        }
    }
}
