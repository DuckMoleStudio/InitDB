package loader;

import com.graphhopper.GHRequest;
import com.graphhopper.GHResponse;
import com.graphhopper.GraphHopper;
import com.graphhopper.config.CHProfile;
import com.graphhopper.config.Profile;
import com.graphhopper.routing.util.EdgeFilter;
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
import java.util.stream.Collectors;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static service.BusStopHSService.listBusStopHSs;
import static service.BusStopHSService.updateBusStopHS;
import static service.BusStopService.*;
import static service.FishnetCell2Service.listFishnetCells2;
import static service.FishnetCell2Service.updateFishnetCell2;
import static service.FishnetCellHSService.listFishnetCellHSs;
import static service.FishnetCellHSService.updateFishnetCellHS;
import static service.FishnetDataService.*;
import static service.FishnetStaticService.listFishnetStatic;
import static service.RouteNameService.listRouteNames;
import static service.RouteService.*;
import static service.TripService.*;
import static service.VersionService.*;

public class Calculation {
    public static void CalcTripHops(double speedRatio, int stopDelay, String osmFile, String dir) {

        final double FakeTime = 60;
        final double FakeDistance = 700;

        long startTime = System.currentTimeMillis();
        int badCount=0;

        List<Route> routes = listRoutes();


        // GH preparation
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("fastest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"), new CHProfile("car2"));
        hopper.importOrLoad();

        for(Route route: routes) {

            double totalTime = 0;
            double totalDistance = 0;

            List<Double> hops = new ArrayList<>();
            for (int i = 0; i < route.getStops().length - 1; i++) {

                // now get coordinates & run GH
                BusStop stop1 = getBusStopById(Integer.parseInt(route.getStops()[i]));
                BusStop stop2 = getBusStopById(Integer.parseInt(route.getStops()[i + 1]));

                double destLon = stop2.getGeom().getCoordinate().getX();
                double destLat = stop2.getGeom().getCoordinate().getY();

                double startLon = stop1.getGeom().getCoordinate().getX();
                double startLat = stop1.getGeom().getCoordinate().getY();

                GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                req.setProfile("car1");
                req.addPoint(new GHPoint(startLat, startLon));
                req.addPoint(new GHPoint(destLat, destLon));

                //req.setCurbsides(Arrays.asList("right", "right"));
                req.putHint("instructions", false);
                req.putHint("calc_points", false);
                //req.putHint(Parameters.Routing.FORCE_CURBSIDE, false);

                GHResponse res = hopper.route(req);

                if (res.hasErrors()) {
                    System.out.println(route.getStops()[i] + " -> " + route.getStops()[i+1]);
                    hops.add(FakeTime);
                    totalTime += FakeTime;
                    totalTime += stopDelay;
                    totalDistance += FakeDistance;
                    badCount++;
                    //throw new RuntimeException(res.getErrors().toString());
                }
                else
                {
                    hops.add((double) (res.getBest().getTime() / 1000 * speedRatio));
                    totalTime += (double) (res.getBest().getTime() / 1000 * speedRatio);
                    totalTime += stopDelay;
                    totalDistance += res.getBest().getDistance();
                }
            }
            double[] dd = new double[hops.size()];
            for(int i=0;i< hops.size();i++) dd[i]=hops.get(i);
            route.setHops(dd);
            route.setTotalTime(totalTime);
            route.setTotalDistance(totalDistance);
            updateRoute(route);
        }
        System.out.printf("\n\n===== Calculated hop times for %d routes in %d seconds with %d errors ======\n\n"
                , routes.size(), (System.currentTimeMillis()-startTime)/1000, badCount);
    }

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

        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("fastest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true).putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"), new CHProfile("car2"));
        hopper.importOrLoad();

        for(Trip trip: trips) {

            double totalTime = 0;
            double totalDistance = 0;

            List<Double> hops = new ArrayList<>();
            for (int i = 0; i < trip.getStops().length - 1; i++) {

                // now get coordinates & run GH
                BusStop stop1 = getBusStopById(Integer.parseInt(trip.getStops()[i]));
                BusStop stop2 = getBusStopById(Integer.parseInt(trip.getStops()[i + 1]));

                double destLon = stop2.getGeom().getCoordinate().getX();
                double destLat = stop2.getGeom().getCoordinate().getY();

                double startLon = stop1.getGeom().getCoordinate().getX();
                double startLat = stop1.getGeom().getCoordinate().getY();

                GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                req.setProfile("car1");
                req.addPoint(new GHPoint(startLat, startLon));
                req.addPoint(new GHPoint(destLat, destLon));

                //req.setCurbsides(Arrays.asList("right", "right"));
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
                    hops.add((double) (res.getBest().getTime() / 1000 * speedRatio));
                    totalTime += (double) (res.getBest().getTime() / 1000 * speedRatio);
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

    public static void CalcStopMinDistToMetro() {

        long startTime = System.currentTimeMillis();

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<BusStop> stops = listBusStops();
        for(BusStop stop: stops)
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

            updateBusStop(stop);
        }

        System.out.printf("\n\n===== Calculated distances for %d stops in %d seconds ======\n\n"
                , stops.size(), (System.currentTimeMillis()-startTime)/1000);
    }

    public static void CalcStopMinDistToMetroVer() {

        long startTime = System.currentTimeMillis();

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<BusStopHS> stops = listBusStopHSs();
        for(BusStopHS stop: stops)
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

            updateBusStopHS(stop);
        }

        System.out.printf("\n\n===== Calculated distances for %d stops in %d seconds ======\n\n"
                , stops.size(), (System.currentTimeMillis()-startTime)/1000);
    }

    public static void CalcStopToMetro(int MetroCriteria, int StopDelay, int PedestrianSpeed, int IntervalDummy)
    {
        long startTime = System.currentTimeMillis();

        List<BusStop> stops = listBusStops();
        List<Route> routes = listRoutes();
        Map<Integer, BusStop> stopMap = new HashMap<>();

        for(BusStop stop: stops)
        {
            stop.setTripSimple(0);
            stop.setTripFull(0);
            stop.setActive(false);
            stopMap.put(stop.getId(), stop);
        }

        for(Route route: routes)
        {
            List<String> routeStops = new ArrayList<>();
            if(route.getStops()!=null) // some routes may be crippled
                routeStops = Arrays.asList(route.getStops());

            double interval = route.getInterval()/2;
            if(Double.isNaN(interval)) interval = IntervalDummy;

            for (String curStopId: routeStops)
            {
                BusStop curStop = stopMap.get(Integer.parseInt(curStopId));
                curStop.setActive(true);

                if(curStop!=null
                        &&routeStops.indexOf(curStopId)<routeStops.size()-1
                        &&curStop.getMinMetroDist()>MetroCriteria) // not metro & not last & not null (bad data may occur)
                {
                    BusStop nextStop;
                    int cc = routeStops.indexOf(curStopId);
                    double tripSimple=0;
                    double tripFull=0;
                    boolean reached=false;
                    while (!reached&&cc<routeStops.size()-1)
                    {
                        tripSimple+=(route.getHops()[cc]+StopDelay);
                        cc++;
                        nextStop = stopMap.get(Integer.parseInt(routeStops.get(cc)));
                        if(nextStop!=null&&nextStop.getMinMetroDist()<=MetroCriteria) // reached!
                        {
                            if(curStop.getTripSimple()==0||curStop.getTripSimple()>tripSimple)
                            {
                                curStop.setTripSimple(tripSimple);
                                curStop.setNearestMetro(routeStops.get(cc));
                            }
                            tripFull=tripSimple+interval+nextStop.getMinMetroDist()*PedestrianSpeed;

                            if(curStop.getTripFull()==0||curStop.getTripFull()>tripFull)
                                curStop.setTripFull(tripFull);

                            reached=true;
                        }
                    }
                }
            }
        }

        //process special cases
        for (BusStop busStop: stops)
        {
            if(busStop.getMinMetroDist()<=MetroCriteria)
            {
                busStop.setNearestMetro(String.valueOf(busStop.getId()));
                busStop.setTripFull(busStop.getMinMetroDist()*PedestrianSpeed);
            }
            else if(busStop.getTripSimple()==0)
            {
                busStop.setTripSimple(Double.POSITIVE_INFINITY);
                busStop.setTripFull(Double.POSITIVE_INFINITY);
            }
            updateBusStop(busStop);
        }
        System.out.printf("\n\n===== Updated %d bus stops in %d seconds ======\n\n"
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

        List<BusStopHS> stops = listBusStopHSs();
        List<RouteName> routes = listRouteNames();
        Map<Integer, BusStopHS> stopMap = new HashMap<>();
        List<Trip> trips = new ArrayList<>();

        // get all trips for version
        for(RouteName route: routes)
        {
            List<Integer> tripsVersion = route.getTrips().get(versionId);
            if(tripsVersion!=null)
            for (Integer ii : tripsVersion)
                trips.add(getTripById(ii));
        }


        for(BusStopHS stop: stops)
        {
            stop.getTripSimple().put(versionId,0d); //?
            stop.getTripFull().put(versionId,0d); //?
            stop.getActive().put(versionId,false);
            stopMap.put(stop.getId(), stop);
        }

        for(Trip trip: trips)
        {
            List<String> routeStops = new ArrayList<>();
            if(trip.getStops()!=null) // some routes may be crippled
                routeStops = Arrays.asList(trip.getStops());

            //set terminals
            stopMap.get(Integer.parseInt(routeStops.get(0))).setTerminal(true);
            stopMap.get(Integer.parseInt(routeStops.get(routeStops.size()-1))).setTerminal(true);

            double interval = trip.getInterval()/2;
            if(Double.isNaN(interval)) interval = IntervalDummy;

            for (String curStopId: routeStops)
            {
                BusStopHS curStop = stopMap.get(Integer.parseInt(curStopId));
                curStop.getActive().put(versionId,true);

                if(curStop!=null
                        &&routeStops.indexOf(curStopId)<routeStops.size()-1
                        &&curStop.getMinMetroDist()>MetroCriteria) // not metro & not last & not null (bad data may occur)
                {
                    BusStopHS nextStop;
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
        for (BusStopHS busStop: stops)
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
            updateBusStopHS(busStop);
        }
        System.out.printf("\n\n===== Updated %d bus stops in %d seconds ======\n\n"
                , stops.size(), (System.currentTimeMillis()-startTime)/1000);
    }

    public static void CalcCellMinDistToStop() {

        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<FishnetCell2> cells = listFishnetCells2();
        for(FishnetCell2 cell: cells)
        {
            Query query = session.createQuery(
                    "select distance(transform(p.geom,98568), transform(:cell,98568)) as d from BusStop p " +
                            "where dwithin(p.geom, :cell, 0.03) = true and p.active = true " +
                            "order by distance(transform(p.geom,98568), transform(:cell,98568))",
                    Double.class);
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setMaxResults(1);
            List<Double> distances = query.getResultList();

            if(!distances.isEmpty())
                cell.setMinStopDist(distances.get(0));
            else cell.setMinStopDist(Double.POSITIVE_INFINITY);

            updateFishnetCell2(cell);
        }



        System.out.printf("\n\n===== Calculated distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }

    public static void CalcCellMinDistToStopVer(Version version) {

        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        List<FishnetData> dataList = new ArrayList<>();

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<FishnetStatic> cells = listFishnetStatic();
        for(FishnetStatic cell: cells)
        {
            Query query = session.createQuery(
                    "select distance(transform(p.geom,98568), transform(:cell,98568)) as d from BusStop p " +
                            "where dwithin(p.geom, :cell, 0.03) = true and p.active = true " +
                            "order by distance(transform(p.geom,98568), transform(:cell,98568))",
                    Double.class);
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setMaxResults(1);
            List<Double> distances = query.getResultList();

            FishnetData fishnetData = new FishnetData();
            FishnetVersionKey key = new FishnetVersionKey(cell,version);
            fishnetData.setId(key);

            if(!distances.isEmpty())
                fishnetData.setMinStopDist(distances.get(0));
            else fishnetData.setMinStopDist(Double.POSITIVE_INFINITY);

            dataList.add(fishnetData);
        }

        version.setFishnetDataList(dataList);
        addVersion(version);


        System.out.printf("\n\n===== Calculated distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }

    public static void CalcCellMinDistToStopHS(int versionId) {

        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<FishnetCellHS> cells = listFishnetCellHSs();
        for(FishnetCellHS cell: cells)
        {
            Query query = session.createQuery(
                    "select distance(transform(p.geom,98568), transform(:cell,98568)) as d from BusStopHS p " +
                            "where dwithin(p.geom, :cell, 0.03) = true and p.active = true " +
                            "order by distance(transform(p.geom,98568), transform(:cell,98568))",
                    Double.class);
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setMaxResults(1);
            List<Double> distances = query.getResultList();

            if(!distances.isEmpty())
                cell.getMinStopDist().put(versionId,distances.get(0));

            else cell.getMinStopDist().put(versionId,Double.POSITIVE_INFINITY);

            updateFishnetCellHS(cell);
        }



        System.out.printf("\n\n===== Calculated distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }


    public static void CalcCellMetroAll(int Radius,
                                        int RadiusMetro,
                                        int PedestrianSpeed,
                                        String osmFile,
                                        String dir,
                                        double speedRatio,
                                        int SnapDistance)
    {
        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<FishnetCell2> cells = listFishnetCells2();

        // GH preparation
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("fastest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true)
                        .putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"), new CHProfile("car2"));
        hopper.importOrLoad();

        for(FishnetCell2 cell: cells)
        {
            Query query = session.createQuery(
                    "select p.tripSimple, p.tripFull, p.nearestMetro," +
                            "distance(transform(p.geom,98568), transform(:cell,98568)) from BusStop p " +
                            "where dwithin(transform(p.geom,98568), transform(:cell,98568), :radius) " +
                            "= true and p.active = true");
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
                    if((Double)stop[0]<minSimple)
                        minSimple=(Double)stop[0];

                    if((Double)stop[1]+(Double)stop[3]*PedestrianSpeed<minFull)
                    {
                        minFull=(Double)stop[1]+(Double)stop[3]*PedestrianSpeed;
                        nearest=(String) stop[2];
                    }
                }
                cell.setMetroSimple(minSimple);
                cell.setMetroFull(minFull);
                cell.setNearestMetro(nearest);
            }
            else
            {
                cell.setMetroSimple(Double.POSITIVE_INFINITY);
                cell.setMetroFull(Double.POSITIVE_INFINITY);
                cell.setNearestMetro("no stops");
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

                cell.setMetroCar(Double.POSITIVE_INFINITY);
                boolean isRoad = true;

                if (!metros.isEmpty()) {
                    for (Metro metro : metros) {
                        // now get coordinates & run GH
                        double destLon = metro.getGeom().getCoordinate().getX();
                        double destLat = metro.getGeom().getCoordinate().getY();

                        double distance;

                        GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                        req.setProfile("car1");
                        req.addPoint(new GHPoint(startLat, startLon));
                        req.addPoint(new GHPoint(destLat, destLon));

                        GHResponse res = hopper.route(req);

                        if (res.hasErrors()) {
                            isRoad = false;
                            //throw new RuntimeException(res.getErrors().toString());
                        } else
                        {
                            distance = res.getBest().getTime() / 1000 * speedRatio;
                            if (distance < cell.getMetroCar())
                            {
                                cell.setMetroCar(distance);
                                cell.setNearestMetroCar(String.valueOf(metro.getId()));
                            }
                        }
                    }

                    if (!isRoad)
                    {
                        cell.setMetroCar(Double.POSITIVE_INFINITY);
                        cell.setNearestMetroCar("no nothing");
                    }
                } else {
                    cell.setMetroCar(Double.POSITIVE_INFINITY);
                    cell.setNearestMetroCar("no metro");
                }
            }
            else
            {
                cell.setMetroCar(Double.POSITIVE_INFINITY);
                cell.setNearestMetroCar("no road");
            }

            updateFishnetCell2(cell);
        }

        System.out.printf("\n\n===== Calculated metro distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }

    public static void CalcCellMetroAllVer(int Radius,
                                        int RadiusMetro,
                                        int PedestrianSpeed,
                                        String osmFile,
                                        String dir,
                                        double speedRatio,
                                        int SnapDistance,
                                           Version version)
    {
        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<FishnetStatic> staticCells = listFishnetStatic();

        List<FishnetData> dataCells = new ArrayList<>();

        // GH preparation
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("fastest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true)
                        .putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"), new CHProfile("car2"));
        hopper.importOrLoad();

        for(FishnetStatic cell: staticCells)
        {
            Query query = session.createQuery(
                    "select p.tripSimple, p.tripFull, p.nearestMetro," +
                            "distance(transform(p.geom,98568), transform(:cell,98568)) from BusStop p " +
                            "where dwithin(transform(p.geom,98568), transform(:cell,98568), :radius) " +
                            "= true and p.active = true");
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setParameter("radius", Radius);
            List<Object[]> stops = (List<Object[]>)query.list();

            List<Double> distances = new ArrayList<>();
            FishnetData dataCell = new FishnetData();
            FishnetVersionKey key = new FishnetVersionKey(cell,version);
            dataCell.setId(key);

            if(!stops.isEmpty())
            {
                double minSimple = Double.POSITIVE_INFINITY;
                double minFull = Double.POSITIVE_INFINITY;
                String nearest = "no routes";
                for(Object[] stop: stops)
                {
                    distances.add((Double)stop[3]);

                    if((Double)stop[0]<minSimple)
                        minSimple=(Double)stop[0];

                    if((Double)stop[1]+(Double)stop[3]*PedestrianSpeed<minFull)
                    {
                        minFull=(Double)stop[1]+(Double)stop[3]*PedestrianSpeed;
                        nearest=(String) stop[2];
                    }
                }
                dataCell.setMetroSimple(minSimple);
                dataCell.setMetroFull(minFull);
                dataCell.setNearestMetro(nearest);

                Collections.sort(distances);
                dataCell.setMinStopDist(distances.get(0));
            }
            else
            {
                dataCell.setMetroSimple(Double.POSITIVE_INFINITY);
                dataCell.setMetroFull(Double.POSITIVE_INFINITY);
                dataCell.setNearestMetro("no stops");
                dataCell.setMinStopDist(Double.POSITIVE_INFINITY);
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

                dataCell.setMetroCar(Double.POSITIVE_INFINITY);
                boolean isRoad = true;

                if (!metros.isEmpty()) {
                    for (Metro metro : metros) {
                        // now get coordinates & run GH
                        double destLon = metro.getGeom().getCoordinate().getX();
                        double destLat = metro.getGeom().getCoordinate().getY();

                        double distance;

                        GHRequest req = new GHRequest().setAlgorithm(Parameters.Algorithms.ASTAR_BI);
                        req.setProfile("car1");
                        req.addPoint(new GHPoint(startLat, startLon));
                        req.addPoint(new GHPoint(destLat, destLon));

                        GHResponse res = hopper.route(req);

                        if (res.hasErrors()) {
                            isRoad = false;
                            //throw new RuntimeException(res.getErrors().toString());
                        } else
                        {
                            distance = res.getBest().getTime() / 1000 * speedRatio;
                            if (distance < dataCell.getMetroCar())
                            {
                                dataCell.setMetroCar(distance);
                                dataCell.setNearestMetroCar(String.valueOf(metro.getId()));
                            }
                        }
                    }

                    if (!isRoad)
                    {
                        dataCell.setMetroCar(Double.POSITIVE_INFINITY);
                        dataCell.setNearestMetroCar("no nothing");
                    }
                } else {
                    dataCell.setMetroCar(Double.POSITIVE_INFINITY);
                    dataCell.setNearestMetroCar("no metro");
                }
            }
            else
            {
                dataCell.setMetroCar(Double.POSITIVE_INFINITY);
                dataCell.setNearestMetroCar("no road");
            }

            dataCells.add(dataCell);
        }

        version.setFishnetDataList(dataCells);
        addVersion(version);

        session.close();
        System.out.printf("\n\n===== Calculated metro distances for %d cells in %d seconds ======\n\n"
                , staticCells.size(), (System.currentTimeMillis()-startTime)/1000);
    }

    public static void CalcCellMetroAllHS(int Radius,
                                        int RadiusMetro,
                                        int PedestrianSpeed,
                                        String osmFile,
                                        String dir,
                                        double speedRatio,
                                        int SnapDistance,
                                          int versionId)
    {
        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        List<FishnetCellHS> cells = listFishnetCellHSs();

        // GH preparation
        GraphHopper hopper = new GraphHopper();
        hopper.setOSMFile(osmFile);
        hopper.setGraphHopperLocation(dir);

        hopper.setProfiles(
                new Profile("car1").setVehicle("car").setWeighting("fastest").setTurnCosts(false),
                new Profile("car2").setVehicle("car").setWeighting("fastest").setTurnCosts(true)
                        .putHint("u_turn_costs", 60)
        );
        hopper.getCHPreparationHandler().setCHProfiles(new CHProfile("car1"), new CHProfile("car2"));
        hopper.importOrLoad();

        for(FishnetCellHS cell: cells)
        {
            Query query = session.createQuery(
                    "select p.tripSimple, p.tripFull, p.nearestMetro, p.active, " +
                            "distance(transform(p.geom,98568), transform(:cell,98568)) from BusStopHS p " +
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
                    /*
                    Map<Integer,Double> mm = (Map<Integer,Double>)stop[0];
                    for(Map.Entry<Integer,Double> me: mm.entrySet())
                        System.out.printf("%s -> %s\n", me.getKey(), me.getValue());

                     */

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

                            if (curFull < minFull) {
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
                        req.setProfile("car1");
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

            updateFishnetCellHS(cell);
        }

        System.out.printf("\n\n===== Calculated metro distances for %d cells in %d seconds ======\n\n"
                , cells.size(), (System.currentTimeMillis()-startTime)/1000);
    }


}
