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
import routing.service.greedyAlgos.DemoV90;
import routing.service.greedyAlgos.LinkV00;
import routing.service.greedyAlgos.PointV01;
import routing.service.greedyAlgos.PointV011;
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
        String jsonInputFile = "C:\\matrix\\data\\zao4_mtrx.json";
        String binInputFile = "C:\\matrix\\data\\zao4_mtrx.bin";

        //OSM data
        String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";


        String algo = "V011"; // algo to perform
        LocalTime workStart = LocalTime.parse("06:00");
        LocalTime workEnd = LocalTime.parse("22:00");
        boolean isGood = false; // with access from R curbside, for GPX. True for "good" files, false for rest
        int capacity = 50;
        int iterations = 200; // these 2 for jsprit algo
        boolean single = false;

        boolean dbExport = false; // export to postgres
        boolean newVersion = true;
        boolean editVersion = false;
        int useVersion = 6;
        boolean ghExport = false;

        int admZone = 6; // 6 for ZAO
        int radius = 500; // from cell to metro


        String urlOutputFile = "C:\\matrix\\data\\out\\zao3.txt";
        String arrOutputFile = "C:\\Users\\User\\Documents\\GD\\a2.txt";
        String outDir = "C:\\Users\\User\\Documents\\GD\\tracks\\a3";
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
            cellStopPattern.getStopsNearest().put(cell, (List<Object[]>) query.list());
        }

        // ------- RUN ALGO -------

        long elTime = System.currentTimeMillis();
        Result rr = new Result();


        if (single) {

            switch (algo) {
                case "V00":
                    V00params params = new V00params(6, 11, 3,
                            4, 3, 100, true, true);
                    rr = LinkV00.Calculate(wayPointList, matrix, params, cellStopPattern);
                    break;

                case "V01":
                    V01params params01 = new V01params(200,7,5, 100, true, true, false);
                    rr = PointV01.Calculate(wayPointList, matrix, params01, cellStopPattern);
                    break;

                case "V011":
                    V01params params011 = new V01params(100,4,5, 100, true, true, false);
                    rr = PointV011.Calculate(wayPointList, matrix, params011, cellStopPattern);
                    break;

                case "V90":
                    V90params params90 = new V90params(false,true,true,true);
                    rr = DemoV90.Calculate(wayPointList, matrix, params90, cellStopPattern);
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
                    TreeMap<KPIs, V00params> resultMap = new TreeMap<>(new KPIcomparator());

                    V00params params;

                    for (int i = 5; i < 8; i++)
                        for (int j = 10; j < 12; j++)
                            for (int k = 3; k < 5; k++)
                                for (int l = 2; l < 5; l++)
                                    for (int m = 2; m < 5; m++) {
                                        params = new V00params(i, j, k, l, m, 100, false, false);
                                        rr = LinkV00.Calculate(wayPointList, matrix, params, cellStopPattern);

                                        // ----- EVALUATE ---------

                                        KPIs kpis = eval(rr, matrix, cellStopPattern);
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
                    break;
                case "V01":

                    TreeMap<KPIs, V01params> resultMap01 = new TreeMap<>(new KPIcomparator());

                    V01params params01;

                    for (int i = 1; i < 6; i++)
                        for (int j = 1; j < 6; j++)
                             {
                                        params01 = new V01params(200,i, j, 100, false, true,false);
                                        rr = PointV01.Calculate(wayPointList, matrix, params01, cellStopPattern);

                                        // ----- EVALUATE ---------

                                        KPIs kpis = eval(rr, matrix, cellStopPattern);
                                        resultMap01.put(kpis, params01);
                                    }

                    for (Map.Entry<KPIs, V01params> me : resultMap01.entrySet()) {
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

                    TreeMap<KPIs, V01params> resultMap011 = new TreeMap<>(new KPIcomparator());

                    V01params params011;

                    for (int i = 1; i < 6; i++)
                        for (int j = 1; j < 6; j++)
                            for (int k = 1; k < 10; k++)
                        {
                            params011 = new V01params(k*100, i, j, 100, false, true,false);
                            rr = PointV011.Calculate(wayPointList, matrix, params011, cellStopPattern);

                            // ----- EVALUATE ---------

                            KPIs kpis = eval(rr, matrix, cellStopPattern);
                            resultMap011.put(kpis, params011);
                        }

                    for (Map.Entry<KPIs, V01params> me : resultMap011.entrySet()) {
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