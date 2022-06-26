package routing;

import entity.FishnetCellVer;
import entity.Version;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import routing.entity.WayPoint;
import routing.entity.eval.*;
import routing.entity.result.Result;
import routing.entity.storage.MatrixLineMap;
import routing.fileManagement.LoadMatrixB;
import routing.fileManagement.WriteGH;
import routing.service.algos.*;
import utils.HibernateSessionFactoryUtil;

import java.sql.Date;
import java.time.LocalTime;
import java.util.*;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static routing.service.Evaluate.eval;
import static routing.service.ExportDB.ExportRoutes;
import static service.AdmzoneService.getAdmzoneById;
import static service.VersionService.getVersionById;
import static service.VersionService.updateVersion;

public class RouteGeneration {
    public static void main(String[] args)
    {
        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\Users\\User\\Documents\\matrix\\data\\zao_active_noU_mtrx.json";
        String binInputFile = "C:\\Users\\User\\Documents\\matrix\\data\\zao_active_noU_mtrx.bin";

        //OSM data
        String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";


        String algo = "V21"; // algo to perform
        //LocalTime workStart = LocalTime.parse("06:00");
        //LocalTime workEnd = LocalTime.parse("22:00");
        boolean isGood = false; // with access from R curbside, for GPX. True for "good" files, false for rest

        boolean single = true;

        boolean dbExport = false; // export to postgres
        boolean newVersion = true;
        boolean editVersion = false;
        int useVersion = 6;
        boolean ghExport = true;

        int admZone = 6; // 6 for ZAO
        int radius = 500; // from cell to metro


        String urlOutputFile = "C:\\Users\\User\\Documents\\matrix\\zao3.txt";
        //String arrOutputFile = "C:\\Users\\User\\Documents\\GD\\a2.txt";
        //String outDir = "C:\\Users\\User\\Documents\\GD\\tracks\\a3";
        // ----- CONTROLS END ---------


        // ------- RESTORE MATRIX FROM JSON FILE ---------

        List<WayPoint> wayPointList = new ArrayList<>();
        Map<WayPoint, MatrixLineMap> matrix = new HashMap<>();
        try {
            LoadMatrixB.restoreB(wayPointList, matrix, jsonInputFile, binInputFile);
        } catch (Exception e) {
            System.out.println("Invalid data file provided");
            return;
        }

        // ------- CREATE EVALUATION PATTERN -----

        Map<Integer,WayPoint> wayPointMap = new HashMap<>();
        for(WayPoint wp: wayPointList)
            wayPointMap.put((int) wp.getIndex(),wp);

        CellStopPattern cellStopPattern = new CellStopPattern();

        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery(
                "select p from FishnetCellVer p " +
                        "where intersects(p.geom, :area) = true ", FishnetCellVer.class);
        query.setParameter("area", getAdmzoneById(admZone).getGeom());
        cellStopPattern.setCells(query.getResultList());

        for(FishnetCellVer cell: cellStopPattern.getCells())
        {
            query = session.createQuery(
                    "select p.id, " +
                            "distance(transform(p.geom,98568), transform(:cell,98568)) from BusStopVer p " +
                            "where dwithin(transform(p.geom,98568), transform(:cell,98568), :radius) = true");
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setParameter("radius", radius);
            List<Object[]> stopsNearest = (List<Object[]>) query.list();
            cellStopPattern.getStopsNearest().put(cell, stopsNearest);

            cellStopPattern.getStopsForCell().put(cell,new ArrayList<>());
            for(Object[] stopDB: stopsNearest)
            {
                if(wayPointMap.containsKey((int) stopDB[0]))
                {
                    WayPoint stopWP = wayPointMap.get((int) stopDB[0]);
                    if(!cellStopPattern.getCellsForStop().containsKey(stopWP))
                        cellStopPattern.getCellsForStop().put(stopWP,new ArrayList<>());
                    cellStopPattern.getCellsForStop().get(stopWP).add(cell);
                    cellStopPattern.getStopsForCell().get(cell).add(stopWP);
                }
            }
        }

        System.out.println("Evaluation pattern created");

        // ------- RUN ALGO -------

        long elTime = System.currentTimeMillis();
        Result rr = new Result();

        AlgoParams algoParams = new AlgoParams();

        if (single) {

            switch (algo) {
                case "V00":
                    algoParams.setMinDistance(6);
                    algoParams.setMaxDistance(11);
                    algoParams.setSiteRadius(3);
                    algoParams.setAddNoLessNewStops(1);
                    algoParams.setRemoveWithLessUnique(1);
                    algoParams.setLog(true);
                    algoParams.setOnePair(true);
                    rr = LinkV00.Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V01":
                    algoParams.setSiteRadius(200);
                    algoParams.setMinTerminalGap(7);
                    algoParams.setRemoveWithLessUnique(5);
                    algoParams.setLog(true);
                    algoParams.setOnePair(true);
                    algoParams.setOnlyMetro(false);
                    rr = PointV01.Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V011":
                    algoParams.setSiteRadius(100);
                    algoParams.setMinTerminalGap(4);
                    algoParams.setRemoveWithLessUnique(3);
                    algoParams.setLog(true);
                    algoParams.setOnePair(true);
                    algoParams.setOnlyMetro(false);
                    rr = PointV011.Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V11":
                    algoParams.setSiteRadius(90);
                    algoParams.setMinTerminalGap(2);
                    algoParams.setRemoveWithLessUnique(4);
                    algoParams.setLog(true);
                    algoParams.setOnePair(true);
                    algoParams.setOnlyMetro(false);
                    algoParams.setMaxDetour(5);
                    algoParams.setPopToDiscard(20000);
                    algoParams.setPop(true);
                    algoParams.setReverseDetour(1.5);
                    rr = new V11().Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V12":
                    algoParams.setSiteRadius(90);
                    algoParams.setMinDistance(7);
                    algoParams.setMaxDistance(10);
                    algoParams.setRemoveWithLessUnique(5);
                    algoParams.setLog(true);
                    algoParams.setOnePair(true);
                    algoParams.setOnlyMetro(false);
                    algoParams.setMaxDetour(5);
                    rr = new V12().Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V21":
                    algoParams.setCapacity(100);
                    algoParams.setIterations(5);
                    algoParams.setSiteRadius(90);
                    algoParams.setMaxDistance(15);
                    algoParams.setMinTerminalGap(2);
                    algoParams.setRemoveWithLessUnique(4);
                    algoParams.setLog(true);
                    algoParams.setOnePair(true);
                    algoParams.setOnlyMetro(true);
                    algoParams.setMaxDetour(5);
                    algoParams.setPopToDiscard(20000);
                    algoParams.setPop(true);
                    algoParams.setReverseDetour(1.5);
                    rr = new V21().Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V90":
                    algoParams.setFrom(true);
                    algoParams.setTo(true);
                    algoParams.setLog(true);
                    algoParams.setOnlyMetro(false);
                    rr = DemoV90.Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V020":
                    algoParams.setCapacity(1);
                    algoParams.setIterations(5);
                    algoParams.setLog(true);
                    rr = SchrimpfV20.Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V021":
                    algoParams.setMinDistance(5);
                    algoParams.setMaxDistance(7);
                    algoParams.setSiteRadius(1);
                    //algoParams.setAddNoLessNewStops(4);
                    algoParams.setRemoveWithLessUnique(3);
                    algoParams.setOnePair(true);

                    algoParams.setCapacity(20);
                    algoParams.setIterations(4);
                    algoParams.setLog(true);
                    rr = SchrimpfV21.Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V022":
                    algoParams.setMinDistance(4);
                    algoParams.setMaxDistance(7);
                    algoParams.setSiteRadius(90);
                    //algoParams.setAddNoLessNewStops(4);
                    algoParams.setRemoveWithLessUnique(3);
                    algoParams.setOnePair(true);

                    algoParams.setCapacity(20);
                    algoParams.setIterations(4);
                    algoParams.setLog(true);
                    rr = SchrimpfV22.Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                case "V023":
                    algoParams.setMinDistance(3);
                    algoParams.setMaxDistance(7);
                    algoParams.setSiteRadius(90);
                    //algoParams.setAddNoLessNewStops(4);
                    algoParams.setRemoveWithLessUnique(3);
                    algoParams.setOnePair(true);

                    algoParams.setCapacity(20);
                    algoParams.setIterations(20);
                    algoParams.setLog(true);
                    rr = SchrimpfV23.Calculate(wayPointList, matrix, algoParams, cellStopPattern);
                    break;

                default:
                    System.out.println("No such algo!");
            }

            long elapsedTime = System.currentTimeMillis() - elTime;
            System.out.println("\nCalculated in: " + elapsedTime / 1000 + " s\n");

            // ----- SAVE RESULTS FOR VISUALISATION ------

            if(ghExport) WriteGH.write(rr, urlOutputFile);

            // ----- SAVE TO MODEL IN POSTGRES -----

            if(dbExport)
            {
                int versionId = useVersion;
                if(newVersion)
                {
                    Version version = new Version();
                    version.setDesc("Demo.V90 ЗАО: от остановки к метро");
                    version.setDate(Date.valueOf("2022-5-11"));
                    updateVersion(version);
                    versionId = version.getVersionId();
                }
                if(editVersion)
                {
                    Version version = getVersionById(versionId);
                    version.setDesc("Alg.V00 ЗАО: минимальная длина маршрутной сети");
                    version.setDate(Date.valueOf("2022-4-24"));
                    updateVersion(version);
                }

                ExportRoutes(versionId, rr, matrix, osmFile, dir);
            }


        } else {
            switch (algo) {
                case "V00":
                    TreeMap<KPIs, AlgoParams> resultMap = new TreeMap<>(new KPIcomparator());
                    for (int i = 5; i < 8; i++)
                        for (int j = 10; j < 12; j++)
                            for (int k = 3; k < 5; k++)
                                for (int l = 2; l < 5; l++)
                                    for (int m = 2; m < 5; m++) {
                                        algoParams.setMinDistance(i);
                                        algoParams.setMaxDistance(j);
                                        algoParams.setSiteRadius(k);
                                        algoParams.setAddNoLessNewStops(l);
                                        algoParams.setRemoveWithLessUnique(m);
                                        algoParams.setOnePair(false);
                                        rr = LinkV00.Calculate(wayPointList, matrix, algoParams, cellStopPattern);

                                        // ----- EVALUATE ---------

                                        KPIs kpis = eval(rr, matrix, cellStopPattern);
                                        resultMap.put(kpis, algoParams);
                                    }

                    for (Map.Entry<KPIs, AlgoParams> me : resultMap.entrySet()) {
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
                    break;
                case "V01":

                    TreeMap<KPIs, AlgoParams> resultMap01 = new TreeMap<>(new KPIcomparator());

                    for (int i = 1; i < 6; i++)
                        for (int j = 1; j < 6; j++)
                             {
                                 algoParams.setSiteRadius(200);
                                 algoParams.setMinTerminalGap(i);
                                 algoParams.setRemoveWithLessUnique(j);
                                 algoParams.setOnePair(true);
                                 algoParams.setOnlyMetro(false);
                                        rr = PointV01.Calculate(wayPointList, matrix, algoParams, cellStopPattern);

                                        // ----- EVALUATE ---------

                                        KPIs kpis = eval(rr, matrix, cellStopPattern);
                                        resultMap01.put(kpis, algoParams);
                                    }

                    for (Map.Entry<KPIs, AlgoParams> me : resultMap01.entrySet()) {
                        System.out.printf("\n\nFor params radius: %d  remove: %d",

                                me.getValue().getMinTerminalGap(),
                                me.getValue().getRemoveWithLessUnique()
                        );
                        System.out.println("\nKPI #1: " + me.getKey().getCellToStop());
                        System.out.println("KPI #2: " + me.getKey().getCellToMetroSimple());
                        System.out.println("KPI #3: " + me.getKey().getCellToMetroFull());
                        System.out.println(me.getKey().getRouteCount() + " trips");
                        System.out.println(me.getKey().getStopCount() + " stops used");
                        System.out.println("total distance: " + me.getKey().getTotalDistance() / 1000);
                    }

                    elapsedTime = System.currentTimeMillis() - elTime;
                    System.out.printf("\n%d variants calculated in: %d seconds", resultMap01.size(), elapsedTime / 1000);
                    break;

                case "V011":

                    TreeMap<KPIs, AlgoParams> resultMap011 = new TreeMap<>(new KPIcomparator());

                    for (int i = 1; i < 6; i++)
                        for (int j = 2; j < 6; j++)
                            for (int k = 6; k < 7; k++)
                        {
                            algoParams = new AlgoParams();
                            algoParams.setSiteRadius(k*100);
                            algoParams.setMinTerminalGap(i);
                            algoParams.setRemoveWithLessUnique(j);
                            algoParams.setOnePair(true);
                            algoParams.setOnlyMetro(false);
                            rr = PointV011.Calculate(wayPointList, matrix, algoParams, cellStopPattern);

                            // ----- EVALUATE ---------

                            KPIs kpis = eval(rr, matrix, cellStopPattern);
                            resultMap011.put(kpis, algoParams);
                        }

                    for (Map.Entry<KPIs, AlgoParams> me : resultMap011.entrySet()) {
                        System.out.printf("\n\nFor params site: %d distance: %d  remove: %d",
                                me.getValue().getSiteRadius(),
                                me.getValue().getMinTerminalGap(),
                                me.getValue().getRemoveWithLessUnique()
                        );
                        System.out.println("\nKPI #1: " + me.getKey().getCellToStop());
                        System.out.println("KPI #2: " + me.getKey().getCellToMetroSimple());
                        System.out.println("KPI #3: " + me.getKey().getCellToMetroFull());
                        System.out.println(me.getKey().getRouteCount() + " trips");
                        System.out.println(me.getKey().getStopCount() + " stops used");
                        System.out.println("total distance: " + me.getKey().getTotalDistance() / 1000);
                    }

                    elapsedTime = System.currentTimeMillis() - elTime;
                    System.out.printf("\n%d variants calculated in: %d seconds", resultMap011.size(), elapsedTime / 1000);
                    break;

                default:
                    System.out.println("No such algo!");
            }
        }
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