import entity.FishnetCell;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static service.FishnetCellService.getFishnetCellById;
import static service.FishnetCellService.updateFishnetCell;

public class ImportPopulation {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();

        try (BufferedReader br = new BufferedReader(new FileReader("c:/matrix/population/202001.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                FishnetCell cell = getFishnetCellById(values[1]);
                if (cell != null)
                {
                    cell.setHome(Integer.parseInt(values[2]));
                    cell.setWork(Integer.parseInt(values[3]));

                    updateFishnetCell(cell);
                    count++;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.printf("\n\n===== Populated %d fishnet cells in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }
}
