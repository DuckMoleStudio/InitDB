import entity.Admzone;
import org.locationtech.jts.geom.*;

import java.util.Arrays;
import java.util.List;

import static service.AdmstripService.getAdmstripById;
import static service.AdmzoneService.*;
import static service.TrzoneService.getTrzoneById;

public class CreateAdmarea {
    public static void main(String[] args) {

        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        //deleteAdmzone(getAdmzoneById(11));
        //deleteAdmzone(getAdmzoneById(5));


     /*   List<Integer> trzs = Arrays.asList(new Integer[]{565,570,563,569,560,567,568,578,572,579,
        571,566,573,576,575,577,574,580,631,623,564});
        List<Integer> adzs = Arrays.asList(new Integer[]{42,128,29,129,104,76,103,127});

        List<Integer> trzs = Arrays.asList(new Integer[]{});
        List<Integer> adzs = Arrays.asList(new Integer[]{23,50,71,125,1,40,135,32,37,146});

        List<Integer> trzs = Arrays.asList(new Integer[]{});
        List<Integer> adzs = Arrays.asList(new Integer[]{28,26,65,63,64,136,137,138,62,14,134,72,33,12,11,73});

        List<Integer> trzs = Arrays.asList(new Integer[]{336,337,338,339,340,341});
        List<Integer> adzs = Arrays.asList(new Integer[]{48,67,143,78,124,106,44,52,55,36});

        List<Integer> trzs = Arrays.asList(new Integer[]{});
        List<Integer> adzs = Arrays.asList(new Integer[]{118,13,57,25,111,35,20,101,117,77,34,69,16});

        List<Integer> trzs = Arrays.asList(new Integer[]{});
        List<Integer> adzs = Arrays.asList(new Integer[]{49,10,112,51,3,75,15,54,53,141,6,109,74,105,2,145});

        List<Integer> trzs = Arrays.asList(new Integer[]{});
        List<Integer> adzs = Arrays.asList(new Integer[]{27,31,19,9,47,131,24,18,38,126,116,5,133,108,8});

        List<Integer> trzs = Arrays.asList(new Integer[]{});
        List<Integer> adzs = Arrays.asList(new Integer[]{113,142,79,121,139,132});*/

        List<Integer> trzs = Arrays.asList(new Integer[]{});
        List<Integer> adzs = Arrays.asList(new Integer[]{102,4,130,123,58,41,7,122,30,144});

        Geometry mp = gf.createPolygon();

        for(Integer i: trzs)
            mp= mp.union(getTrzoneById(i).getGeom());
        for(Integer i: adzs)
            mp= mp.union(getAdmstripById(i).getGeom());

        Admzone admzone = new Admzone();
        admzone.setName("Центральный административный округ");
        Polygon[] ps= new Polygon[1];
        ps[0]= (Polygon) mp;
        admzone.setGeom(gf.createMultiPolygon(ps));
        //admzone.setGeom((MultiPolygon) mp);

        updateAdmzone(admzone);
    }
}
