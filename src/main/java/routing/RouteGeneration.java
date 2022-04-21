package routing;

import entity.Version;
import routing.entity.WayPoint;
import routing.entity.eval.KPIs;
import routing.entity.eval.V00params;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.storage.MatrixLineMap;
import routing.fileManagement.LoadMatrixB;
import routing.fileManagement.WriteGH;
import routing.service.greedyAlgos.LinkV00;

import java.sql.Date;
import java.time.LocalTime;
import java.util.*;

import static java.lang.Math.round;
import static routing.service.Evaluate.eval;
import static routing.service.ExportDB.ExportRoutes;
import static service.VersionService.getVersionById;
import static service.VersionService.updateVersion;

public class RouteGeneration {
    public static void main(String[] args) {

        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\matrix\\data\\zao2_mtrx.json";
        String binInputFile = "C:\\matrix\\data\\zao2_mtrx.bin";

        //OSM data
        String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";


        String algo = "V00"; // algo to perform
        LocalTime workStart = LocalTime.parse("06:00");
        LocalTime workEnd = LocalTime.parse("22:00");
        boolean isGood = false; // with access from R curbside, for GPX. True for "good" files, false for rest
        int capacity = 50;
        int iterations = 200; // these 2 for jsprit algo
        boolean single = true;

        String urlOutputFile = "C:\\matrix\\data\\out\\zao3.txt";
        String arrOutputFile = "C:\\Users\\User\\Documents\\GD\\a2.txt";
        String outDir = "C:\\Users\\User\\Documents\\GD\\tracks\\a3";
        // ----- CONTROLS END ---------


        // ------- RESTORE MATRIX FROM JSON FILE ---------

        List<WayPoint> wayPointList = new ArrayList<>();
        Map<WayPoint, MatrixLineMap> matrix = new HashMap<>();
        try {
            LoadMatrixB.restoreB(wayPointList, matrix, jsonInputFile,binInputFile);
        } catch (Exception e) {
            System.out.println("Invalid data file provided");
            return;
        }


        // ------- RUN ALGO -------

        long elTime = System.currentTimeMillis();
        Result rr = new Result();


        if(single)
        {
            V00params params = new V00params(8, 14, 5, 3, 3, 100, true);
            switch (algo) {
                case "V00":
                    rr = LinkV00.Calculate(wayPointList, matrix, params);
                    break;
            }

/*
        for(Itinerary it: rr.getItineraries())
            System.out.println(it.getId()+" "+it.getName());

 */
            long elapsedTime = System.currentTimeMillis() - elTime;
        //System.out.println("\n\nTotal time: " + round(rr.getTimeTotal()) / 60000 + " min");
        //System.out.println("Total distance: " + round(rr.getDistanceTotal()) / 1000 + " km");
        //System.out.println("Routes: " + rr.getItineraryQty());
        System.out.println("\nCalculated in: " + elapsedTime/1000 + " s\n");

            WriteGH.write(rr, urlOutputFile);


        }
        else {
            TreeMap<KPIs, V00params> resultMap = new TreeMap<>(new KPIcomparator());

            V00params params;

            for (int i = 6; i < 9; i++)
                for (int j = 9; j < 15; j++)
                    for (int k = 3; k < 6; k++)
                        for (int l = 1; l < 5; l++)
                            for (int m = 1; m < 5; m++)
                        {

                        params = new V00params(i, j, k, l,m,100,false);
                        switch (algo) {
                            case "V00":
                                rr = LinkV00.Calculate(wayPointList, matrix, params);
                                break;
                        }




                        // ----- SAVE RESULTS FOR VISUALISATION ------

                        //WriteGH.write(rr, urlOutputFile);
                        //WriteGPX.write(osmFile, dir, outDir, rr, isGood);
                        //WriteArrivals.write(rr, arrOutputFile);

                        // ----- EVALUATE ---------

                        KPIs kpis = eval(rr, matrix);
                        resultMap.put(kpis, params);

                    }

            for (Map.Entry<KPIs, V00params> me : resultMap.entrySet()) {
                System.out.printf("\n\nFor params %d - %d radius: %d add: %d remove: %d",
                        me.getValue().getMinDistance(),
                        me.getValue().getMaxDistance(),
                        me.getValue().getSiteRadius(),
                        me.getValue().getAddNoLessNewStops(),
                        me.getValue().getRemoveWithLessUnique()
                        );
                System.out.println("\nKPI #1: " + me.getKey().getCellToStop());
                System.out.println("KPI #2: " + me.getKey().getCellToMetroSimple());
                System.out.println("KPI #3: " + me.getKey().getCellToMetroFull());
                System.out.println(me.getKey().getRouteCount() + " trips");
                System.out.println(me.getKey().getStopCount() + " stops used");
                System.out.println("total distance: " + me.getKey().getTotalDistance() / 1000);
            }

            long elapsedTime = System.currentTimeMillis() - elTime;
            System.out.printf("\n%d variants calculated in: %d seconds", resultMap.size(), elapsedTime / 1000);
        }

        // ----- SAVE TO MODEL IN POSTGRES -----

      /*  Version version = new Version();
        version.setDesc("Первый алгоритм генерации");
        version.setDate(Date.valueOf("2022-3-26"));
        updateVersion(version);
               */
        //ExportRoutes(6,rr,matrix,osmFile,dir);

        }


}

class KPIcomparator implements Comparator<KPIs>
{
    @Override
    public int compare(KPIs o1, KPIs o2) {
        int diff = (int) (o1.getCellToMetroSimple()-o2.getCellToMetroSimple());
        return Math.abs(diff) < 3? (int) (o2.getTotalDistance() - o1.getTotalDistance()) : diff;
    }
}