import entity.FishnetCell;
import entity.FishnetCellMatrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static service.FishnetCellMatrixService.addCell;
import static service.FishnetCellMatrixService.updateCell;
import static service.FishnetCellService.listFishnetCells;

public class TheMatrix {
    public static void main(String[] args) {

        List<FishnetCell> oldFishnet = listFishnetCells();
        Map<Integer,FishnetCellMatrix> cells = new HashMap<>();
        for(FishnetCell oldCell: oldFishnet)
        {
            FishnetCellMatrix newCell = new FishnetCellMatrix();
            newCell.setId(Integer.parseInt(oldCell.getId()));
            newCell.setGeom(oldCell.getGeom());
            newCell.setDestCells(new HashMap<>());

            cells.put(Integer.parseInt(oldCell.getId()),newCell);
        }

        System.out.println("\nLoaded cells, parsing matrix");


        int countLines=0;
        long startTime = System.currentTimeMillis();

        try (BufferedReader br = new BufferedReader(new FileReader("D:/matrix/03_CMatrix_1_201904.csv")))
        {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");
                countLines++;

                int keyDeparture = Integer.parseInt(values[1]);
                int keyArrival = Integer.parseInt(values[2]);

                if(cells.containsKey(keyArrival))
                {
                    int cnt = Integer.parseInt(values[4]) + Integer.parseInt(values[8]) + Integer.parseInt(values[9]);
                    FishnetCellMatrix curCell = cells.get(keyArrival);
                    curCell.setDepart(curCell.getDepart()+cnt);

                    if(cells.containsKey(keyDeparture))
                    {
                        curCell.getDestCells().merge(keyDeparture,cnt,(k,v)->v+=cnt);


                        /*
                        if(curCell.getDestCells().containsKey(Integer.parseInt(values[2])))
                        {
                            curCell.getDestCells().put(Integer.parseInt(values[2]),
                                    curCell.getDestCells().get(Integer.parseInt(values[2]))+
                                            Integer.parseInt(values[4])); //....
                        }
                        else
                            curCell.getDestCells().put(Integer.parseInt(values[2]),Integer.parseInt(values[4]));

                         */
                    }
                }

                if(countLines%100000000==0) System.out.print(":");
                else
                if(countLines%10000000==0) System.out.print(".");
            }

            System.out.printf("\nParsed %d lines.\n\n",countLines);

        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("\n\nOptimizing...\n\n");

        for(Map.Entry<Integer,FishnetCellMatrix> me: cells.entrySet())
        {
            //me.getValue().setDepart(me.getValue().getDepart()/30);
            //me.getValue().getDestCells().forEach((k,v)->v=v/30);
            me.getValue().getDestCells().entrySet().removeIf(entry -> entry.getValue()<1);
            updateCell(me.getValue());
        }



        System.out.printf("\n\n===== Completed in %d seconds ======\n\n",
                 (System.currentTimeMillis()-startTime)/1000);


    }
}
