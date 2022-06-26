package routing;
import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.BusFlagEncoder;
import com.graphhopper.routing.util.NGPTFlagEncoder;
import com.graphhopper.util.Parameters;
import entity.BusStopVer;
import entity.Metro;
import org.hibernate.Session;
import org.hibernate.query.Query;
import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.storage.MatrixElement;
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;
import routing.fileManagement.SaveMatrixB;
import routing.service.Matrix;
import utils.HibernateSessionFactoryUtil;

import java.time.Duration;
import java.time.LocalTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static routing.service.Matrix.NearestMap;
import static routing.service.Matrix.NearestMapFrom;
import static service.AdmzoneService.getAdmzoneById;


public class InitMatrix {

    // ----- CONTROLS (set before use) -----------

    //OSM data
    static String osmFile = "C:/Users/User/Downloads/RU-MOW.osm.pbf";
    static String dir = "local/graphhopper";

    static String jsonOutputFile = "C:\\Users\\User\\Documents\\matrix\\data\\zao_active_U240_mtrx.json";
    static String binOutputFile = "C:\\Users\\User\\Documents\\matrix\\data\\zao_active_U240_mtrx.bin";

    static LocalTime startTime = LocalTime.parse("06:00");
    static boolean runAlgo = false; // execute itinerary routing algo at this stage?
    static int capacity = 50; // garbage car capacity in abstract units

    static int version = 5;
    static int area = 6;
    static int MetroCriteria = 150; // meters, if stop is within, then it's a metro stop

    // ----- CONTROLS END ---------


    // ----- CREATE WP LIST ------
    static List<WayPoint> wayPointList = new ArrayList<>();

    //static GraphHopper hopper = new GraphHopper();

    public static void main(String[] args)
    {

        //0. init hopper
        /*
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        hopper.getEncodingManagerBuilder().add(new BusFlagEncoder(5,5,1));
        hopper.getEncodingManagerBuilder().add(new NGPTFlagEncoder(5,5,1));

        hopper.setProfiles(
                new Profile("ngpt1").setVehicle("ngpt").setWeighting("fastest").setTurnCosts(false),
                new Profile("ngpt2").setVehicle("ngpt").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60),
                new Profile("bus1").setVehicle("bus").setWeighting("fastest").setTurnCosts(false),
                new Profile("bus2").setVehicle("bus").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60),
                new Profile("car1").setVehicle("car").setWeighting("fastest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(
                new CHProfile("ngpt1"),
                new CHProfile("ngpt2"),
                new CHProfile("bus1"),
                new CHProfile("bus2"),
                new CHProfile("car1"),
                new CHProfile("car2")
        );
        hopper.importOrLoad();

         */

        //1. get bus stops by area filter
        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery(
                "select p from BusStopVer p " +
                        "where within(p.geom, :area) = true ", BusStopVer.class);
        query.setParameter("area", getAdmzoneById(area).getGeom());
        List<BusStopVer> allStops = query.getResultList();
        System.out.printf("\nFrom %d total stops in %s\n", allStops.size(), getAdmzoneById(area).getName());

        List<WayPoint> allMetro = new ArrayList<>();
        List<WayPoint> termMetro = new ArrayList<>();

        //2. filter active by version, map to wp and set type
        for(BusStopVer stop: allStops)
        {
            if(stop.getActive().get(version)) // ||true if we need all, not active
            {
                WayPoint wp = new WayPoint();
                wp.setDescription(stop.getName());
                wp.setIndex(stop.getId());
                wp.setLon(stop.getGeom().getCoordinate().getX());
                wp.setLat(stop.getGeom().getCoordinate().getY());
                wp.setType(WayPointType.STOP);
                if(stop.isTerminal()&&(stop.getMinMetroDist()<MetroCriteria))
                {
                    wp.setType(WayPointType.METRO_TERMINAL);
                    allMetro.add(wp);
                    termMetro.add(wp);
                }
                if(stop.isTerminal()&&!(stop.getMinMetroDist()<MetroCriteria))
                    wp.setType(WayPointType.TERMINAL);
                if(!stop.isTerminal()&&(stop.getMinMetroDist()<MetroCriteria))
                {
                    wp.setType(WayPointType.METRO);
                    allMetro.add(wp);
                }


                //if(stop.getNearestMetro().get(version)!=null) wp.setSchedule(stop.getNearestMetro().get(version));

                wayPointList.add(wp);
            }
        }
        System.out.printf("selected %d active stops\n\n", wayPointList.size());



        // ------ NOW FILL THE MATRIX -----

        Map<WayPoint, MatrixLineMap> matrix = Matrix.FillGHMulti4Map(
                wayPointList, osmFile, dir, true, true);


        // ----- SET METRO ATTRIBUTE FOR STOPS ----

        System.out.println("Now setting metro...");

        /*
        query = session.createQuery(
                "select p from Metro p " +
                        "where within(p.geom, :area) = true ", Metro.class);
        query.setParameter("area", getAdmzoneById(area).getGeom());
        List<Metro> metros = query.getResultList();
        System.out.printf("%d metro exits in %s\n", metros.size(), getAdmzoneById(area).getName());
         */




        // ----- AND SAVE THE MATRIX IN JSON -----

        SaveMatrixB.saveB(wayPointList,matrix,jsonOutputFile,binOutputFile);



        // ----- EXECUTE ITINERARY ALGORITHM IF DESIRED -----
        if(runAlgo)
        {
            //do smth
        }
    }

    /*
    public static TimeDistancePair trace(WayPoint from, WayPoint to, String curb1, String curb2)
    {
        TimeDistancePair tdp = new TimeDistancePair();

        GHRequest req = new GHRequest(
                from.getLat()
                , from.getLon()
                , to.getLat()
                , to.getLon())
                .setProfile("ngpt2")
                .setAlgorithm(Parameters.Algorithms.ASTAR_BI);

            req.setCurbsides(Arrays.asList(curb1, curb2));
            req.putHint("u_turn_costs", 6000);

        req.setSnapPreventions(Arrays.asList("bridge", "tunnel"));
        req.putHint("instructions", false);
        req.putHint("calc_points", false);
        req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

        GHResponse res = hopper.route(req);

        if (res.hasErrors())
        {
            throw new RuntimeException(res.getErrors().toString());
        }

        tdp.setDistance(res.getBest().getDistance());
        tdp.setTime(res.getBest().getTime());

        return tdp;
    }

     */
}
