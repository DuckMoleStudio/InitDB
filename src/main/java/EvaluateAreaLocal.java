import entity.BusStopVer;
import entity.FishnetCellVer;
import entity.Trip;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import routing.entity.WayPoint;
import routing.entity.eval.KPIs;
import routing.entity.result.Itinerary;
import utils.HibernateSessionFactoryUtil;

import java.util.*;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static routing.service.Matrix.TimeBetweenMap;
import static service.AdmzoneService.getAdmzoneById;

public class EvaluateAreaLocal {
    public static void main(String[] args)
    {
        final int MetroCriteria = 150; // meters, if stop is within, then it's a metro stop
        final int StopDelay = 30; // sec, time loss for stopping
        final int PedestrianSpeed = 1; // in m/s, 1 equals 3.6 km/h but we have air distances so ok
        final int IntervalDummy = 600; // in case not available
        final int Radius = 500; // meters, looking for stops in this radius
        final double speedRatio = 2.0;

        KPIs kpis = new KPIs();
        int totalPop = 0;
        int count1 = 0;
        int count2 = 0;
        int count3 = 0;

        int stopCount = 0;
        int versionId = 5;
        int areaId = 6;


        // ---------- STOPS TO METRO ---------------


        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery(
                "select p from BusStopVer p " +
                        "where within(p.geom, :area) = true ", BusStopVer.class);
        query.setParameter("area", getAdmzoneById(areaId).getGeom());
        List<BusStopVer> stops = query.getResultList();

        query = session.createQuery(
                "select p from Trip p " +
                        "where within(p.geomML, :area) = true ", Trip.class);
        query.setParameter("area", getAdmzoneById(areaId).getGeom());
        List<Trip> tripsAll = query.getResultList();

        List<Trip> trips = new ArrayList<>();

        for(Trip trip: tripsAll)
            if(trip.getVersions().containsKey(versionId))
                if(trip.getVersions().get(versionId))
                    trips.add(trip);


        double totDist = 0;


        Map<Integer, BusStopVer> stopMap = new HashMap<>();

        for(BusStopVer stop: stops)
        {
            stop.getTripSimple().put(versionId,0d); //?
            stop.getTripFull().put(versionId,0d); //?
            stop.getActive().put(versionId,false);
            stopMap.put(stop.getId(), stop);
        }

        for(Trip trip: trips)
        {
            totDist+=trip.getTotalDistance();

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

        }
        //System.out.println(stopCount);

        // ------------ CELL TO KPI ---------------

        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);
        query = session.createQuery(
                "select p from FishnetCellVer p " +
                        "where within(p.geom, :area) = true ", FishnetCellVer.class);
        query.setParameter("area", getAdmzoneById(areaId).getGeom());
        List<FishnetCellVer> cells = query.getResultList();

        for(FishnetCellVer cell: cells)
        {
            query = session.createQuery(
                    "select p.id, " +
                            "distance(transform(p.geom,98568), transform(:cell,98568)) from BusStopVer p " +
                            "where dwithin(transform(p.geom,98568), transform(:cell,98568), :radius) = true");
            query.setParameter("cell", gf.createPoint(getCentroid(cell.getGeom())));
            query.setParameter("radius", Radius);
            List<Object[]> stopsNearest = (List<Object[]>)query.list();

            if(!stopsNearest.isEmpty())
            {
                double minSimple = Double.POSITIVE_INFINITY;
                double minFull = Double.POSITIVE_INFINITY;
                String nearest = "no routes";
                for(Object[] stop: stopsNearest)
                {
                    if(stopMap.containsKey((int)stop[0]))
                    {
                        BusStopVer stopCC = stopMap.get((int)stop[0]);

                        double curSimple = stopCC.getTripSimple().get(versionId);
                        double curFull = stopCC.getTripFull().get(versionId) +
                                (Double)stop[1] * PedestrianSpeed;



                        if(stopCC.getActive().get(versionId)!=null)
                        {
                            boolean active = stopCC.getActive().get(versionId);
                            if (active)
                            {
                                nearest = "ok";
                                if (curSimple < minSimple)
                                    minSimple = curSimple;

                                if (curFull < minFull)
                                    minFull = curFull;
                            }
                        }
                    }
                    cell.getMetroSimple().put(versionId,minSimple);
                    cell.getMetroFull().put(versionId,minFull);
                    cell.getNearestMetro().put(versionId,nearest);
                }
            }
            else
            {
                cell.getMetroSimple().put(versionId,Double.POSITIVE_INFINITY);
                cell.getMetroFull().put(versionId,Double.POSITIVE_INFINITY);
                cell.getNearestMetro().put(versionId,"no stops");
            }

            int pop = cell.getHome()+cell.getWork();
            totalPop+=pop;
            if(cell.getNearestMetro().get(versionId).equals("ok")) count1+=pop;
            if(cell.getMetroSimple().get(versionId)<900) count2+=pop;
            if(cell.getMetroFull().get(versionId)<1200) count3+=pop;

        }

        kpis.setCellToStop(100*count1/totalPop);
        kpis.setCellToMetroSimple(100*count2/totalPop);
        kpis.setCellToMetroFull(100*count3/totalPop);
        kpis.setRouteCount(trips.size());
        kpis.setTotalDistance(totDist);
        kpis.setStopCount(stopCount);

        System.out.println("\nRESULT:\n");
        System.out.println("KPI #1: " + kpis.getCellToStop());
        System.out.println("KPI #2: " + kpis.getCellToMetroSimple());
        System.out.println("KPI #3: " + kpis.getCellToMetroFull());
        System.out.println(kpis.getRouteCount() + " trips");
        System.out.println(kpis.getStopCount() + " stops used");
        System.out.println("total distance: " + kpis.getTotalDistance() / 1000);
    }
}
