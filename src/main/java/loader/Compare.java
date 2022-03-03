package loader;

import entity.FishnetCellHS;
import entity.FishnetData;
import entity.FishnetStatic;
import entity.Version;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static service.FishnetCellHSService.listFishnetCellHSs;
import static service.FishnetDataService.getFishnetDataByKey;
import static service.FishnetDataService.listFishnetDataByCell;
import static service.FishnetStaticService.listFishnetStatic;

public class Compare {

    public static void compareCellsByFull(Version v1, Version v2)
    {
        List<FishnetStatic> cells = listFishnetStatic();
        for(FishnetStatic cell: cells)
        {
            Optional<FishnetData> d1 = getFishnetDataByKey(cell,v1);
            Optional<FishnetData> d2 = getFishnetDataByKey(cell,v2);
            if(d1.isPresent()&&d2.isPresent())
                if(d1.get().getMetroFull()!=d2.get().getMetroFull())
                    System.out.printf("%s : %f -> %f\n", cell.getId(), d1.get().getMetroFull(), d2.get().getMetroFull());
        }
    }

    public static void compareCellsHS(int v1, int v2)
    {
        List<FishnetCellHS> cells = listFishnetCellHSs();
        for(FishnetCellHS cell: cells)
        {
            /*
            for(Map.Entry<Integer,Double> me: cell.getMetroFull().entrySet())
                System.out.printf("%s -> %f\n", me.getKey(), me.getValue());

             */

            double dd1=0,dd2=0;
            if(cell.getMetroFull().containsKey(v1)) dd1 = cell.getMetroFull().get(v1);
            if(cell.getMetroFull().containsKey(v2)) dd2 = cell.getMetroFull().get(v2);

            if(dd1!=dd2)
                    System.out.printf("%s : %f -> %f\n", cell.getId(), dd1, dd2);
        }
    }

}
