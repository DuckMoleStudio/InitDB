package loader;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.BusFlagEncoder;
import com.graphhopper.routing.util.EdgeFilter;
import com.graphhopper.routing.util.NGPTFlagEncoder;
import com.graphhopper.storage.index.Snap;
import com.graphhopper.util.Parameters;
import com.graphhopper.util.shapes.GHPoint;
import entity.*;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import utils.HibernateSessionFactoryUtil;

import java.util.*;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static service.AdmzoneService.getAdmzoneById;
import static service.BusStopVerService.*;
import static service.FishnetCellVerService.listFishnetCellVers;
import static service.FishnetCellVerService.updateFishnetCellVer;
import static service.RouteNameService.listRouteNames;
import static service.TripService.*;
import static service.VersionService.*;

public class Calculation {

    public static void CalcTripHopsVer(double speedRatio, int stopDelay, String osmFile, String dir) {

        final double FakeTime = 60;
        final double FakeDistance = 700;

        long startTime = System.currentTimeMillis();
        int badCount=0;

        List<Trip> trips = listTrips();


        // GH preparation
        GraphHopper hopper = new GraphHopper();
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

        for(Trip trip: trips) {

            double totalTime = 0;
            double totalDistance = 0;

            List<Double> hops = new ArrayList<>();
            for (int i = 0; i < trip.getStops().length - 1; i++) {

                //System.out.println(trip.getId() + " ->" + trip.getStops()[i + 1]);
                // now get coordinates & run GH
                BusStopVer stop1 = getBusStopVerById(Integer.parseInt(trip.getStops()[i]));
                BusStopVer stop2 = getBusStopVerById(Integer.parseInt(trip.getStops()[i + 1]));

                double destLon = stop2.getGeom().getCoordinate().getX();
                double destLat = stop2.getGeom().getCoordinate().getY();

                double startLon = stop1.getGeom().getCoordinate().getX();
                double startLat = stop1.getGeom().getCoordinate().getY();

                GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                req.setProfile("ngpt1");
                req.addPoint(new GHPoint(startLat, startLon));
                req.addPoint(new GHPoint(destLat, destLon));

                //req.setCurbsides(Arrays.asList("right", "right"));
                req.setSnapPreventions(Arrays.asList("bridge", "tunnel"));
                req.putHint("instructions", false);
                req.putHint("calc_points", false);
                //req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

                GHResponse res = hopper.route(req);

                if (res.hasErrors()) {
                    System.out.println(trip.getStops()[i] + " -> " + trip.getStops()[i+1]);
                    hops.add(FakeTime);
                    totalTime += FakeTime;
                    totalTime += stopDelay;
                    totalDistance += FakeDistance;
                    badCount++;
                    //throw new RuntimeException(res.getErrors().toString());
                }
                else
                {
                    hops.add(res.getBest().getTime() / 1000 * speedRatio);
                    totalTime += res.getBest().getTime() / 1000 * speedRatio;
                    totalTime += stopDelay;
                    totalDistance += res.getBest().getDistance();
                }
            }
            double[] dd = new double[hops.size()];
            for(int i=0;i< hops.size();i++) dd[i]=hops.get(i);
            trip.setHops(dd);
            trip.setTotalTime(totalTime);
            trip.setTotalDistance(totalDistance);
            updateTrip(trip);
        }
        System.out.printf("\n\n===== Calculated hop times for %d trips in %d seconds with %d errors ======\n\n"
                , trips.size(), (System.currentTimeMillis()-startTime)/1000, badCount);
    }

    public static void CalcStopMinDistToMetroVer() {

        long startTime = System.currentTimeMillis();

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<BusStopVer> stops = listBusStopVers();
        for(BusStopVer stop: stops)
        {
            Query query = session.createQuery(
                    "select distance(transform(p.geom,98568), transform(:stop,98568)) as d from Metro p " +
                            "where dwithin(p.geom, :stop, 0.1) = true " +
                            "order by distance(transform(p.geom,98568), transform(:stop,98568))",
                    Double.class);
            query.setParameter("stop", stop.getGeom());
            query.setMaxResults(1);
            List<Double> distances = query.getResultList();

            if(!distances.isEmpty())
                stop.setMinMetroDist(distances.get(0));
            else stop.setMinMetroDist(Double.POSITIVE_INFINITY);

            updateBusStopVer(stop);
        }

        System.out.printf("\n\n===== Calculated distances for %d stops in %d seconds ======\n\n"
                , stops.size(), (System.currentTimeMillis()-startTime)/1000);
    }

    public static void CalcStopToMetroVer(
            int MetroCriteria,
            int StopDelay,
            int PedestrianSpeed,
            int IntervalDummy,
            int versionId)
    {
        long startTime = System.currentTimeMillis();

        double verTotalDistance = 0;
        int stopCount = 0;

        List<BusStopVer> stops = listBusStopVers();
        List<RouteName> routes = listRouteNames();
        Map<Integer, BusStopVer> stopMap = new HashMap<>();
        List<Trip> trips = new ArrayList<>();

        // get all trips for version
        for(RouteName route: routes)
        {
            List<Integer> tripsVersion = route.getTrips().get(versionId);
            if(tripsVersion!=null)
            for (Integer ii : tripsVersion)
                trips.add(getTripById(ii));
        }


        for(BusStopVer stop: stops)
        {
            stop.getTripSimple().put(versionId,0d); //?
            stop.getTripFull().put(versionId,0d); //?
            stop.getActive().put(versionId,false);
            if(versionId==5)
            stop.setTerminal(false);
            stopMap.put(stop.getId(), stop);
        }

        for(Trip trip: trips)
        {
            verTotalDistance+=trip.getTotalDistance();

            List<String> routeStops = new ArrayList<>();
            if(trip.getStops()!=null) // some routes may be crippled
                routeStops = Arrays.asList(trip.getStops());

            //set terminals
            if(versionId==5)
            {
                stopMap.get(Integer.parseInt(routeStops.get(0))).setTerminal(true);
                stopMap.get(Integer.parseInt(routeStops.get(routeStops.size() - 1))).setTerminal(true);
            }

            double interval = trip.getInterval()/2;
            if(Double.isNaN(interval)) interval = IntervalDummy;

            for (String curStopId: routeStops)
            {
                BusStopVer curStop = stopMap.get(Integer.parseInt(curStopId));
                if(!curStop.getActive().get(versionId)) stopCount++;
                curStop.getActive().put(versionId,true);


                if(curStop!=null
                        &&routeStops.indexOf(curStopId)<routeStops.size()-1
                        &&curStop.getMinMetroDist()>MetroCriteria) // not metro & not last & not null (bad data may occur)
                {
                    BusStopVer nextStop;
                    int cc = routeStops.indexOf(curStopId);
                    double tripSimple=0;
                    double tripFull=0;
                    boolean reached=false;
                    while (!reached&&cc<routeStops.size()-1)
                    {
                        tripSimple+=(trip.getHops()[cc]+StopDelay);
                        cc++;
                        nextStop = stopMap.get(Integer.parseInt(routeStops.get(cc)));
                        if(nextStop!=null&&nextStop.getMinMetroDist()<=MetroCriteria) // reached!
                        {
                            if(curStop.getTripSimple().get(versionId)==0
                                    ||curStop.getTripSimple().get(versionId)>tripSimple)
                            {
                                curStop.getTripSimple().put(versionId,tripSimple);
                                curStop.getNearestMetro().put(versionId,routeStops.get(cc));
                            }
                            tripFull=tripSimple+interval+nextStop.getMinMetroDist()*PedestrianSpeed;

                            if(curStop.getTripFull().get(versionId)==0
                                    ||curStop.getTripFull().get(versionId)>tripFull)
                                curStop.getTripFull().put(versionId,tripFull);

                            reached=true;
                        }
                    }
                }
            }
        }

        //process special cases
        for (BusStopVer busStop: stops)
        {
            if(busStop.getMinMetroDist()<=MetroCriteria)
            {
                busStop.getNearestMetro().put(versionId,String.valueOf(busStop.getId()));
                busStop.getTripFull().put(versionId,busStop.getMinMetroDist()*PedestrianSpeed);
            }
            else if(busStop.getTripSimple().get(versionId)==0)
            {
                busStop.getTripSimple().put(versionId,Double.POSITIVE_INFINITY);
                busStop.getTripFull().put(versionId,Double.POSITIVE_INFINITY);
            }
            updateBusStopVer(busStop);
        }

        Version version = getVersionById(versionId);
        version.setTotalLength(verTotalDistance);
        version.setTripCount(trips.size());
        version.setStopCount(stopCount);
        updateVersion(version);


        System.out.printf("\n\n===== Updated %d bus stops in %d seconds ======\n\n"
                , stops.size(), (System.currentTimeMillis()-startTime)/1000);
    }


    public static void CalcCellMinDistToStopHS(int versionId, boolean filter, int areaId) {

        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();

        Query query;
        List<FishnetCellVer> cells;
        if(filter)
        {
            query = session.createQuery(
                    "select p from FishnetCellVer p " +
                            "where intersects(p.geom, :area) = true ", FishnetCellVer.class);
            query.setParameter("area", getAdmzoneById(areaId).getGeom());
            cells = query.getResultList();
        }
        else
        cells = listFishnetCellVers();


        for(FishnetCellVer cell: cells)
        {
            query = session.createQuery(
                    "select p.active, distance(transform(p.geom,98568), transform(:cell,98568)) as d from BusStopVer p " +
                            "where dwithin(p.geom, :cell, 0.03) = true " +
                            "order by distance(transform(p.geom,98568), transform(:cell,98568))");
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));

            List<Object[]> stops = (List<Object[]>)query.list();

            double dist = Double.POSITIVE_INFINITY;
            if(!stops.isEmpty())
            {
                for(Object[] stop: stops)
                {
                    boolean active = false;
                    if(((Map<Integer,Boolean>)stop[0]).get(versionId)!=null)
                    active = ((Map<Integer,Boolean>)stop[0]).get(versionId);
                    if(active)
                    {
                        dist = (Double) stop[1];
                        break;
                    }
                }
                cell.getMinStopDist().put(versionId, dist);
            }
            updateFishnetCellVer(cell);
        }



        System.out.printf("\n\n===== Calculated distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }

    public static void CalcCellMetroAllHS(int Radius,
                                        int RadiusMetro,
                                        int PedestrianSpeed,
                                        String osmFile,
                                        String dir,
                                        double speedRatio,
                                        int SnapDistance,
                                          int versionId,
                                          boolean filter,
                                          int areaId)
    {
        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();

        Query query;
        List<FishnetCellVer> cells;
        if(filter)
        {
            query = session.createQuery(
                    "select p from FishnetCellVer p " +
                            "where intersects(p.geom, :area) = true ", FishnetCellVer.class);
            query.setParameter("area", getAdmzoneById(areaId).getGeom());
            cells = query.getResultList();
        }
        else
            cells = listFishnetCellVers();

        // GH preparation
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        //hopper.getEncodingManagerBuilder().add(new BusFlagEncoder(5,5,1));
        hopper.getEncodingManagerBuilder().add(new NGPTFlagEncoder(5,5,1));

        hopper.setProfiles(
                new Profile("ngpt1").setVehicle("ngpt").setWeighting("fastest").setTurnCosts(false),
                new Profile("ngpt2").setVehicle("ngpt").setWeighting("fastest").setTurnCosts(true),
                new Profile("ngpt3").setVehicle("ngpt").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60),
                new Profile("ngpt4").setVehicle("ngpt").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 120),
                new Profile("ngpt5").setVehicle("ngpt").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 240),
                //new Profile("bus1").setVehicle("bus").setWeighting("fastest").setTurnCosts(false),
                //new Profile("bus2").setVehicle("bus").setWeighting("fastest").setTurnCosts(true),
                new Profile("car1").setVehicle("car").setWeighting("fastest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 10)
        );
        hopper.getCHPreparationHandler().setCHProfiles(
                new CHProfile("ngpt1"),
                new CHProfile("ngpt2"),
                new CHProfile("ngpt3"),
                new CHProfile("ngpt4"),
                new CHProfile("ngpt5"),
                //new CHProfile("bus1"),
                //new CHProfile("bus2"),
                new CHProfile("car1"),
                new CHProfile("car2")
        );
        hopper.importOrLoad();

        for(FishnetCellVer cell: cells)
        {
            query = session.createQuery(
                    "select p.tripSimple, p.tripFull, p.nearestMetro, p.active, " +
                            "distance(transform(p.geom,98568), transform(:cell,98568)) from BusStopVer p " +
                            "where dwithin(transform(p.geom,98568), transform(:cell,98568), :radius) = true");
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setParameter("radius", Radius);
            List<Object[]> stops = (List<Object[]>)query.list();

            if(!stops.isEmpty())
            {
                double minSimple = Double.POSITIVE_INFINITY;
                double minFull = Double.POSITIVE_INFINITY;
                String nearest = "no routes";
                for(Object[] stop: stops)
                {
                    double curSimple = ((Map<Integer,Double>)stop[0]).get(versionId);
                    double curFull = ((Map<Integer,Double>)stop[1]).get(versionId) +
                            (Double)stop[4] * PedestrianSpeed;



                    if(((Map<Integer,Boolean>)stop[3]).get(versionId)!=null)
                    {
                        boolean active = ((Map<Integer,Boolean>)stop[3]).get(versionId);

                        if (active)
                        {
                            if (curSimple < minSimple)
                                minSimple = curSimple;

                            if (curFull < minFull)
                            {
                                minFull = curFull;
                                nearest = ((Map<Integer,String>)stop[2]).get(versionId);
                            }
                        }
                    }
                }
                cell.getMetroSimple().put(versionId,minSimple);
                cell.getMetroFull().put(versionId,minFull);
                cell.getNearestMetro().put(versionId,nearest);
            }
            else
            {
                cell.getMetroSimple().put(versionId,Double.POSITIVE_INFINITY);
                cell.getMetroFull().put(versionId,Double.POSITIVE_INFINITY);
                cell.getNearestMetro().put(versionId,"no stops");
            }

            // check road availability
            double startLon = getCentroid(cell.getGeom()).getX();
            double startLat = getCentroid(cell.getGeom()).getY();

            Snap snap = hopper.getLocationIndex().findClosest(startLat,startLon, EdgeFilter.ALL_EDGES);
            if(snap.isValid()&&snap.getQueryDistance()<SnapDistance)
            {
                query = session.createQuery(
                        "select p from Metro p " +
                                "where dwithin(transform(p.geom,98568), transform(:cell,98568), :radius) = true ", Metro.class);
                query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
                query.setParameter("radius", RadiusMetro);

                List<Metro> metros = query.getResultList();

                double metroCar = Double.POSITIVE_INFINITY;
                String nearestMetro = "no smth";

                boolean isRoad = true;

                if (!metros.isEmpty()) {
                    for (Metro metro : metros) {
                        // now get coordinates & run GH
                        double destLon = metro.getGeom().getCoordinate().getX();
                        double destLat = metro.getGeom().getCoordinate().getY();

                        double distance;

                        GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                        req.setProfile("ngpt3");
                        req.addPoint(new GHPoint(startLat, startLon));
                        req.addPoint(new GHPoint(destLat, destLon));

                        GHResponse res = hopper.route(req);

                        if (res.hasErrors()) {
                            isRoad = false;
                            //throw new RuntimeException(res.getErrors().toString());
                        } else
                        {
                            distance = res.getBest().getTime() / 1000 * speedRatio;
                            if (distance < metroCar)
                            {
                                metroCar=distance;
                                nearestMetro=String.valueOf(metro.getId());
                            }
                        }
                    }
                    cell.getMetroCar().put(versionId,metroCar);
                    cell.getNearestMetroCar().put(versionId,nearestMetro);

                    if (!isRoad)
                    {
                        cell.getMetroCar().put(versionId,Double.POSITIVE_INFINITY);
                        cell.getNearestMetroCar().put(versionId,"no nothing");
                    }
                } else {
                    cell.getMetroCar().put(versionId,Double.POSITIVE_INFINITY);
                    cell.getNearestMetroCar().put(versionId,"no metro");
                }
            }
            else
            {
                cell.getMetroCar().put(versionId,Double.POSITIVE_INFINITY);
                cell.getNearestMetroCar().put(versionId,"no road");
            }

            updateFishnetCellVer(cell);
        }

        System.out.printf("\n\n===== Calculated metro distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }


}
