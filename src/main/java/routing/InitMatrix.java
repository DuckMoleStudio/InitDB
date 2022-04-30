package routing;
import entity.BusStopVer;
import org.hibernate.Session;
import org.hibernate.query.Query;
import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.storage.MatrixLineMap;
import routing.fileManagement.SaveMatrixB;
import routing.service.Matrix;
import utils.HibernateSessionFactoryUtil;

import java.time.Duration;
import java.time.LocalTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static service.AdmzoneService.getAdmzoneById;


public class InitMatrix {
    public static void main(String[] args) {
        // ----- CONTROLS (set before use) -----------

        //OSM data
        String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";

        String jsonOutputFile = "C:\\matrix\\data\\zao3_mtrx.json";
        String binOutputFile = "C:\\matrix\\data\\zao3_mtrx.bin";

        LocalTime startTime = LocalTime.parse("06:00");
        boolean runAlgo = false; // execute itinerary routing algo at this stage?
        int capacity = 50; // garbage car capacity in abstract units

        int version = 3;
        int area = 6;
        int MetroCriteria = 350; // meters, if stop is within, then it's a metro stop

        // ----- CONTROLS END ---------


        // ----- CREATE WP LIST ------
        List<WayPoint> wayPointList = new ArrayList<>();

        //1. get bus stops by area filter
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery(
                "select p from BusStopVer p " +
                        "where within(p.geom, :area) = true ", BusStopVer.class);
        query.setParameter("area", getAdmzoneById(area).getGeom());
        List<BusStopVer> allStops = query.getResultList();
        System.out.printf("\nFrom %d total stops in %s\n", allStops.size(), getAdmzoneById(area).getName());

        //2. filter active by version, map to wp and set type
        for(BusStopVer stop: allStops)
        {
            if(stop.getActive().get(version)||true)
            {
                WayPoint wp = new WayPoint();
                wp.setDescription(stop.getName());
                wp.setIndex(stop.getId());
                wp.setLon(stop.getGeom().getCoordinate().getX());
                wp.setLat(stop.getGeom().getCoordinate().getY());
                wp.setType(WayPointType.STOP);
                if(stop.isTerminal()&&(stop.getMinMetroDist()<MetroCriteria))
                    wp.setType(WayPointType.METRO_TERMINAL);
                if(stop.isTerminal()&&!(stop.getMinMetroDist()<MetroCriteria))
                    wp.setType(WayPointType.TERMINAL);
                if(!stop.isTerminal()&&(stop.getMinMetroDist()<MetroCriteria))
                    wp.setType(WayPointType.METRO);

                wayPointList.add(wp);
            }
        }
        System.out.printf("selected %d active stops\n\n", wayPointList.size());

        // ------ NOW FILL THE MATRIX -----

        Map<WayPoint, MatrixLineMap> matrix = Matrix.FillGHMulti4Map(
                wayPointList, osmFile, dir, true, true);


        // ----- AND SAVE THE MATRIX IN JSON -----

        SaveMatrixB.saveB(wayPointList,matrix,jsonOutputFile,binOutputFile);


        // ----- EXECUTE ITINERARY ALGORITHM IF DESIRED -----
        if(runAlgo)
        {
            //do smth
        }
    }
}
