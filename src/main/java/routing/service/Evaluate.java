package routing.service;

import entity.BusStopVer;
import entity.FishnetCellVer;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import routing.entity.WayPoint;
import routing.entity.eval.CellStopPattern;
import routing.entity.eval.KPIs;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.storage.MatrixLineMap;
import utils.HibernateSessionFactoryUtil;

import java.util.*;

import static org.locationtech.jts.algorithm.Centroid.getCentroid;
import static routing.service.Matrix.TimeBetweenMap;
import static service.AdmzoneService.getAdmzoneById;

public class Evaluate {

    public static KPIs eval(Result result, Map<WayPoint, MatrixLineMap> matrix, CellStopPattern cellStopPattern)
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
        int versionId = 0;

        double effTotalDistance = 0;
        int effTrips = 0;

        long startTime = System.currentTimeMillis();

        // ---------- STOPS TO METRO ---------------


        Session session = HibernateSessionFactoryUtil.getSessionFactory().openSession();
        Query query = session.createQuery(
                "select p from BusStopVer p " +
                        "where within(p.geom, :area) = true ", BusStopVer.class);
        query.setParameter("area", getAdmzoneById(6).getGeom());
        List<BusStopVer> stops = query.getResultList();

        Map<Integer, BusStopVer> stopMap = new HashMap<>();

        for(BusStopVer stop: stops)
        {
            stop.getTripSimple().put(versionId,0d); //?
            stop.getTripFull().put(versionId,0d); //?
            stop.getActive().put(versionId,false);
            stopMap.put(stop.getId(), stop);
        }

        for(Itinerary itinerary: result.getItineraries())
        {
            effTotalDistance += itinerary.getDistance();
            effTrips++;
            List<WayPoint> routeStops = itinerary.getWayPointList();

            for (WayPoint wp: routeStops)
            {
                BusStopVer curStop = stopMap.get((int)wp.getIndex());
                if(!curStop.getActive().get(versionId)) stopCount++;
                curStop.getActive().put(versionId,true);

                if(curStop!=null
                        &&routeStops.indexOf(wp)<routeStops.size()-1
                        &&curStop.getMinMetroDist()>MetroCriteria) // not metro & not last & not null (bad data may occur)
                {
                    BusStopVer nextStop;
                    int cc = routeStops.indexOf(wp);
                    double tripSimple=0;
                    double tripFull=0;
                    boolean reached=false;
                    while (!reached&&cc<routeStops.size()-1)
                    {
                        tripSimple+=(TimeBetweenMap(routeStops.get(cc),routeStops.get(cc+1),matrix)*speedRatio/1000
                                +StopDelay);
                        cc++;
                        nextStop = stopMap.get((int)routeStops.get(cc).getIndex());
                        if(nextStop!=null&&nextStop.getMinMetroDist()<=MetroCriteria) // reached!
                        {
                            if(curStop.getTripSimple().get(versionId)==0
                                    ||curStop.getTripSimple().get(versionId)>tripSimple)
                            {
                                curStop.getTripSimple().put(versionId,tripSimple);
                                curStop.getNearestMetro().put(versionId,routeStops.get(cc).getDescription());
                            }
                            tripFull=tripSimple+IntervalDummy+nextStop.getMinMetroDist()*PedestrianSpeed;

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
        long t1 = System.currentTimeMillis();
        //System.out.println("stage 1: "+(t1-startTime));

        // ------------ CELL TO KPI ---------------


        List<FishnetCellVer> cells = cellStopPattern.getCells();

        for(FishnetCellVer cell: cells)
        {
            List<Object[]> stopsNearest = cellStopPattern.getStopsNearest().get(cell);

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

            //int pop = cell.getHome()+cell.getWork();
            int pop = cell.getHome();
            totalPop+=pop;
            if(cell.getNearestMetro().get(versionId).equals("ok")) count1+=pop;
            if(cell.getMetroSimple().get(versionId)<1200) count2+=pop;
            if(cell.getMetroFull().get(versionId)<1200) count3+=pop;

        }

        kpis.setCellToStop(100*count1/totalPop);
        kpis.setCellToMetroSimple(100*count2/totalPop);
        kpis.setCellToMetroFull(100*count3/totalPop);
        kpis.setRouteCount(result.getItineraryQty());
        kpis.setTotalDistance(result.getDistanceTotal());
        //kpis.setRouteCount(effTrips);
        //kpis.setTotalDistance(effTotalDistance);
        kpis.setStopCount(stopCount);

        long t2 = System.currentTimeMillis();
        //System.out.println("stage 2: "+(t2-t1));

        return  kpis;
    }
}
