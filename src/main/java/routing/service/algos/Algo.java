package routing.service.algos;

import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import entity.FishnetCellVer;
import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.eval.AlgoParams;
import routing.entity.eval.CellStopPattern;
import routing.entity.eval.KPIs;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.storage.Hop;
import routing.entity.storage.MatrixElement;
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;

import java.util.*;

import static routing.service.Matrix.*;

public abstract class Algo
{
    protected static Map<WayPoint, MatrixLineMap> matrix;
    protected static List<WayPoint> wayPoints = new ArrayList<>();
    protected static Result result;
    protected static Map<WayPoint,Integer> visitCount = new HashMap<>();
    protected static int discardCount = 0;

    protected static AlgoParams params;

    protected static List<WayPoint> terminalsAll = new ArrayList<>();
    protected static List<WayPoint> terminalsMetro = new ArrayList<>();
    protected static List<WayPoint> stopsOnly = new ArrayList<>();

    protected static CellStopPattern cellStopPattern;

    public static int itCount=0;

    public abstract Result Calculate(
            List<WayPoint> wayPointList,
            Map<WayPoint, MatrixLineMap> matrixIn,
            AlgoParams params,
            CellStopPattern cellStopPattern);

    protected static void InitTerminals()
   {
       List<WayPoint> allMetro = new ArrayList<>();
       List<WayPoint> terminalsOnly = new ArrayList<>();

       // classify points
       for(WayPoint wp: wayPoints)
       {
           if (wp.getType() == WayPointType.TERMINAL)
               terminalsOnly.add(wp);
           if (wp.getType() == WayPointType.METRO_TERMINAL)
               allMetro.add(wp);
           if (wp.getType() == WayPointType.STOP)
               stopsOnly.add(wp);
       }

       // group in clusters
       Map<WayPoint,Set<WayPoint>> clusters = new HashMap<>();
       for(WayPoint wp: allMetro)
       {
           Set<WayPoint> cluster;
           if(!clusters.containsKey(wp))
           {
               cluster = new HashSet<>(List.of(wp));
               clusters.put(wp, cluster);
           }
           else
               cluster = clusters.get(wp);
           for(WayPoint wpTry: allMetro)
           {
               double distTo = TimeBetweenMap(wp,wpTry,matrix);
               double distFrom = TimeBetweenMap(wpTry,wp,matrix);
               if((distTo<params.getSiteRadius()*1000 || distFrom<params.getSiteRadius()*1000)
                       && !clusters.containsKey(wpTry))
               {
                   cluster.add(wpTry);
                   clusters.put(wpTry,cluster);
               }
           }
       }

       // get the best terminal stop for each cluster
       Set<Set<WayPoint>> clusterSet = new HashSet<>(clusters.values());

       if(params.isLog())
       {
           System.out.printf("\n%d clusters total for %d metro", clusterSet.size(), allMetro.size());

           for (Set<WayPoint> cluster : clusterSet)
           {
               System.out.println("\n\nCluster:");
               for (WayPoint wp : cluster)
                   System.out.printf("%s (%d)\n", wp.getDescription(), wp.getIndex());
           }
       }

       Map<WayPoint,List<WayPoint>> toMetro = new HashMap<>();

       for (Set<WayPoint> cluster: clusterSet)
       {
           WayPoint metro = null;
           double minTotalTime = Double.POSITIVE_INFINITY;
           for(WayPoint wp: cluster)
           {
               double totalTime = 0;
               for (WayPoint wpO : cluster)
                   if(!wpO.equals(wp))
                       totalTime+=TimeBetweenMap(wpO,wp,matrix);
               if(totalTime<minTotalTime)
               {
                   minTotalTime=totalTime;
                   metro=wp;
               }
           }
           if(minTotalTime<Double.POSITIVE_INFINITY)
           {
               terminalsMetro.add(metro);
               terminalsAll.add(metro);
           }
       }

       // add plain terminals if required
       if(!params.isOnlyMetro())
           terminalsAll.addAll(terminalsOnly);
   }

    protected static void FillLink(Itinerary itinerary, boolean filter, int repCount)
    {
        double totalTime = 0;
        double totalDistance = 0;

        TreeMap<Double, Hop> hopMap = new TreeMap<>();

        List<WayPoint> allWP = itinerary.getWayPointList();
        for (int i = 0; i < allWP.size()-1; i++)
        {
            double curDistance = DistanceBetweenMap(allWP.get(i), allWP.get(i+1), matrix);
            double curTime = TimeBetweenMap(allWP.get(i), allWP.get(i+1), matrix);
            Hop link = new Hop(allWP.get(i),allWP.get(i+1));
            hopMap.put(curDistance, link);
            totalDistance+=curDistance;
            totalTime+=curTime;
        }


        while (!hopMap.isEmpty())
        {
            // get longest hop
            double oldDistance = hopMap.lastKey();
            Hop oldHop = hopMap.get(oldDistance);
            double oldTime = TimeBetweenMap(oldHop.getFrom(),oldHop.getTo(),matrix);
            hopMap.remove(oldDistance);

            // get matrix line sorted by distance
            MatrixLineMap mlFrom = matrix.get(oldHop.getFrom());
            TreeMap<Double, WayPoint> distancesFrom = new TreeMap<>();
            for(Map.Entry<WayPoint, TimeDistancePair> tempME: mlFrom.getDistances().entrySet())
                if(tempME.getValue().getDistance()!=Double.POSITIVE_INFINITY) // ??
                    distancesFrom.put(tempME.getValue().getDistance(),tempME.getKey());

            // try to route thru nearest possible
            boolean found = false;
            while (!distancesFrom.isEmpty()&&!found)
            {
                WayPoint tryWP = distancesFrom.get(distancesFrom.firstKey());
                double newDistance = distancesFrom.firstKey() + DistanceBetweenMap(
                        tryWP,
                        oldHop.getTo(),
                        matrix);
                double newTime = TimeBetweenMap(oldHop.getFrom(), tryWP, matrix)
                        + TimeBetweenMap(tryWP, oldHop.getTo(), matrix);

                // if new hops are no longer than old*K and wp is valid
                boolean valid = !filter || (visitCount.get(tryWP)<=repCount);
                if(((newTime-oldTime)<params.getMaxDetour()*1000)
                        &&!itinerary.getWayPointList().contains(tryWP)&&valid)
                {
                    //new 2 hops
                    visitCount.put(tryWP,visitCount.get(tryWP)+1);
                    hopMap.put(distancesFrom.firstKey(),
                            new Hop(oldHop.getFrom(), tryWP));
                    hopMap.put(DistanceBetweenMap(tryWP, oldHop.getTo(), matrix),
                            new Hop(tryWP, oldHop.getTo()));

                    totalDistance += newDistance;
                    totalDistance -= oldDistance;

                    totalTime += newTime;
                    totalTime -= oldTime;

                    int index = itinerary.getWayPointList().indexOf(oldHop.getFrom()); // add to itinerary
                    itinerary.getWayPointList().add(index + 1, tryWP);
                    found = true;
                }
                distancesFrom.remove(distancesFrom.firstKey());
            }
        }
        // complete itinerary
        itinerary.setDistance(totalDistance);
        itinerary.setTime(totalTime);
    }

    protected static Itinerary buildItinerary(List<WayPoint> wayPointsIt)
    {
        Itinerary itinerary = new Itinerary();

        WayPoint start = wayPointsIt.get(0);
        WayPoint end = wayPointsIt.get(wayPointsIt.size()-1);

        itinerary.setName(start.getDescription() + " - " + end.getDescription());

        itinerary.setWayPointList(wayPointsIt);

        for(WayPoint wp: wayPointsIt)
            visitCount.put(wp, visitCount.get(wp) + 1);

        // distance & time will be calculated & set here
        FillLink(itinerary, false, 0);

        itinerary.setId(itCount++);

        return itinerary;
    }

    protected static void printKPI(KPIs kpis, String message)
    {
        System.out.printf("\n%s\n\n",message);
        System.out.println("KPI #1: " + kpis.getCellToStop());
        System.out.println("KPI #2: " + kpis.getCellToMetroSimple());
        System.out.println("KPI #3: " + kpis.getCellToMetroFull());
        System.out.println(kpis.getRouteCount() + " trips");
        System.out.println(kpis.getStopCount() + " stops used");
        System.out.println("total distance: " + kpis.getTotalDistance() / 1000);
    }

    protected static void removeItinerary(Itinerary it)
    {
        for(WayPoint wp: it.getWayPointList())
            visitCount.put(wp,visitCount.get(wp)-1);
        result.getItineraries().remove(it);
        result.setDistanceTotal(result.getDistanceTotal() - it.getDistance());
        result.setTimeTotal(result.getTimeTotal() - it.getTime());
        result.setItineraryQty(result.getItineraryQty()-1);
        discardCount++;

        //System.out.println("-- "+it.getId());
    }


    protected static void DiscardByStops()
    {
        for(Itinerary ii: new ArrayList<>(result.getItineraries()))
        {
            int redCount = 0;
            for (WayPoint wp : ii.getWayPointList())
                if (visitCount.get(wp) < 2) redCount++;
            if(redCount<params.getRemoveWithLessUnique())
                removeItinerary(ii);
        }
    }

    protected static void DiscardByPop()
    {
        for(Itinerary ii: new ArrayList<>(result.getItineraries()))
        {
            int redCount = 0;
            for (WayPoint wp : ii.getWayPointList())
                if (visitCount.get(wp) < 2)
                {
                    int uniquePop = 0;
                    for(FishnetCellVer cell: cellStopPattern.getCellsForStop().get(wp))
                    {
                        int visitSum = 0;
                        for(WayPoint stopForCell: cellStopPattern.getStopsForCell().get(cell))
                        {
                            if(!stopForCell.equals(wp))
                                if(ii.getWayPointList().contains(stopForCell))
                                    visitSum+=(visitCount.get(stopForCell)-1);
                                else
                                    visitSum+=visitCount.get(stopForCell);
                        }
                        if(visitSum!=0) uniquePop+=cell.getHome();
                    }
                    redCount+=uniquePop;
                }

            if(redCount<params.getPopToDiscard())
                removeItinerary(ii);
        }

    }

    protected static boolean isTo(WayPoint wp, List<WayPoint> destinations)
    {
        MatrixElement nearestToME = NearestMap(wp,matrix,destinations);
        WayPoint nearestTo = nearestToME.getWayPoint();
        if(nearestTo!=null)
        return nearestToME.getTime()<TimeBetweenMap(nearestTo,wp,matrix);
        else
            return false;
    }

}
