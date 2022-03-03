import entity.FishnetCell;
import entity.FishnetCellHS;

import java.util.List;

import static service.FishnetCellHSService.addFishnetCellHS;
import static service.FishnetCellHSService.updateFishnetCellHS;
import static service.FishnetCellService.listFishnetCells;

public class LoadFishnetCellHS {
    public static void main(String[] args) {

        List<FishnetCell> oldFishnet = listFishnetCells();

        for(FishnetCell cell: oldFishnet)
        {
            FishnetCellHS newCell = new FishnetCellHS();
            newCell.setId(Integer.parseInt(cell.getId()));
            newCell.setHome(cell.getHome());
            newCell.setWork(cell.getWork());
            newCell.setGeom(cell.getGeom());

            updateFishnetCellHS(newCell);
        }
    }
}
