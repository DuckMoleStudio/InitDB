package loader;

import entity.FishnetData;
import entity.FishnetStatic;
import entity.Version;

import java.util.List;
import java.util.Optional;

import static service.FishnetDataService.getFishnetDataByKey;
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

}
