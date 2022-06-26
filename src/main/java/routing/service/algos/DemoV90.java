package routing.service.algos;

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

import static routing.service.Evaluate.eval;
import static routing.service.Matrix.*;

public class DemoV90
{

    public static Map<WayPoint, MatrixLineMap> matrix;
    public static Result result;
    public static Map<WayPoint,Integer> visitCount = new HashMap<>();
    public static int discardCount;

    public static Result Calculate(
            List<WayPoint> wayPoints,
            Map<WayPoint, MatrixLineMap> matrixIn,
            AlgoParams params,
            CellStopPattern cellStopPattern)
    {
        matrix = matrixIn;
        for(WayPoint wp: wayPoints)
            visitCount.put(wp,0);

        result = new Result();
        result.setMethodUsed("Demo Algorithm, visualize links to metro, V.90");

        discardCount = 0;
        List<WayPoint> terminals = new ArrayList<>();

        // GET TERMINALS
        for(WayPoint wp: wayPoints)
        {
            boolean term;
            if(params.isOnlyMetro())
                term = wp.getType()== WayPointType.METRO_TERMINAL;
            else
                term = wp.getType()==WayPointType.METRO_TERMINAL||wp.getType()==WayPointType.TERMINAL;
            if (term)
                terminals.add(wp);
        }

        int itCount=0;

        // MAKE A ROUTE FOR EACH STOP
        for(WayPoint wp: wayPoints)
            if(!terminals.contains(wp))
            {
                MatrixElement startME = NearestMapFrom(wp, matrix, terminals);
                MatrixElement endME = NearestMap(wp, matrix, terminals);
                WayPoint start = startME.getWayPoint();
                WayPoint end = endME.getWayPoint();

                if(startME.getTime()<TimeBetweenMap(wp,start,matrix)&&params.isTo())
                {
                    // build trip start->wp
                    Itinerary itinerary = new Itinerary();
                    itinerary.setName(start.getDescription() + " - " + wp.getDescription());

                    itinerary.getWayPointList().add(start);
                    itinerary.getWayPointList().add(wp);

                    itinerary.setDistance(DistanceBetweenMap(start, wp, matrix));
                    itinerary.setTime(TimeBetweenMap(start, wp, matrix));


                    visitCount.put(start, visitCount.get(start) + 1);
                    visitCount.put(wp, visitCount.get(wp) + 1);


                    FillLink(itinerary, 50, false, 0);

                    itinerary.setId(itCount);
                    itinerary.setRoute(itCount++);
                    itinerary.setDir(1);


                    result.getItineraries().add(itinerary);
                    result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
                    result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
                    result.setItineraryQty(result.getItineraryQty() + 1);
                }

                if(endME.getTime()<TimeBetweenMap(end,wp,matrix)&&params.isFrom())
                {
                    // build trip wp->end
                    Itinerary itinerary = new Itinerary();
                    itinerary.setName(wp.getDescription() + " - " + end.getDescription());

                    itinerary.getWayPointList().add(wp);
                    itinerary.getWayPointList().add(end);

                    itinerary.setDistance(DistanceBetweenMap(wp, end, matrix));
                    itinerary.setTime(TimeBetweenMap(wp, end, matrix));

                    visitCount.put(wp, visitCount.get(wp) + 1);
                    visitCount.put(end, visitCount.get(end) + 1);

                    FillLink(itinerary, 50, false, 0);

                    itinerary.setId(itCount);
                    itinerary.setRoute(itCount++);
                    itinerary.setDir(0);

                    result.getItineraries().add(itinerary);
                    result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
                    result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
                    result.setItineraryQty(result.getItineraryQty() + 1);
                }
            }


        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "BEFORE:");


        // DISCARD REDUNDANT
        for(Itinerary ii: new ArrayList<>(result.getItineraries()))
        {
            int redCount = 0;
            for (WayPoint wp : ii.getWayPointList())
                if (visitCount.get(wp) < 2) redCount++;
            if(redCount<1)
                removeItinerary(ii);
        }

        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "AFTER DISCARDING:");





        return result;
    }


    public static void FillLink(Itinerary itinerary,
                                double k,
                                boolean filter,
                                int repCount
    )
    {
        double totalTime = 0, totalDistance = 0;

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
            hopMap.remove(oldDistance);

            // get matrix line sorted by distance
            MatrixLineMap mlFrom = matrix.get(oldHop.getFrom());
            TreeMap<Double, WayPoint> distancesFrom = new TreeMap<>();
            for(Map.Entry<WayPoint, TimeDistancePair> tempME: mlFrom.getDistances().entrySet())
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

                // if new hops are no longer than old*K and wp is valid
                boolean valid = !filter || (visitCount.get(tryWP)<=repCount);
                if(((newDistance-oldDistance)<k)&&!itinerary.getWayPointList().contains(tryWP)&&valid)
                {
                    //new 2 hops
                    visitCount.put(tryWP,visitCount.get(tryWP)+1);
                    hopMap.put(distancesFrom.firstKey(),
                            new Hop(oldHop.getFrom(), tryWP));
                    totalTime+=TimeBetweenMap(oldHop.getFrom(),tryWP,matrix);

                    hopMap.put(DistanceBetweenMap(tryWP, oldHop.getTo(), matrix),
                            new Hop(tryWP, oldHop.getTo()));
                    totalTime+=TimeBetweenMap(tryWP, oldHop.getTo(), matrix);

                    totalDistance += newDistance;
                    totalDistance -= oldDistance;

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

    public static void printKPI(KPIs kpis, String message)
    {
        System.out.printf("\n%s\n\n",message);
        System.out.println("KPI #1: " + kpis.getCellToStop());
        System.out.println("KPI #2: " + kpis.getCellToMetroSimple());
        System.out.println("KPI #3: " + kpis.getCellToMetroFull());
        System.out.println(kpis.getRouteCount() + " trips");
        System.out.println(kpis.getStopCount() + " stops used");
        System.out.println("total distance: " + kpis.getTotalDistance() / 1000);
    }

    public static void removeItinerary(Itinerary it)
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

}
