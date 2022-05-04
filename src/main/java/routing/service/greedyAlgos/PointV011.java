package routing.service.greedyAlgos;

import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.eval.KPIs;
import routing.entity.eval.V01params;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.result.TrackList;
import routing.entity.storage.Hop;
import routing.entity.storage.MatrixElement;
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;

import java.util.*;

import static routing.service.Evaluate.eval;
import static routing.service.Matrix.*;

public class PointV011 {


    public static Map<WayPoint, MatrixLineMap> matrix;
    public static Result result;
    public static Map<WayPoint,Integer> visitCount = new HashMap<>();
    public static int discardCount;

    public static Map<WayPoint,TrackList> nodes = new HashMap<>();

    public static Result Calculate(
            List<WayPoint> wayPoints,
            Map<WayPoint, MatrixLineMap> matrixIn,
            V01params params)
    {
        matrix = matrixIn;
        for(WayPoint wp: wayPoints)
            visitCount.put(wp,0);

        result = new Result();
        result.setMethodUsed("Simple Algorithm, linking stops V.011");

        discardCount = 0;
        List<Hop> routes = new ArrayList<>(); // index is route number
        Map<Hop, Integer> routeF = new HashMap<>(); // counters for trips
        Map<Hop, Integer> routeR = new HashMap<>();
        List<Hop> links = new ArrayList<>();
        List<WayPoint> terminals = new ArrayList<>();
        Map<Hop, TrackList> trips = new HashMap<>();



        // GET TERMINALS
        Map<WayPoint,List<WayPoint>> plan = new HashMap<>();
        for(WayPoint wp: wayPoints)
        {
            boolean term;
            if(params.isOnlyMetro())
                term = wp.getType()==WayPointType.METRO_TERMINAL;
            else
                term = wp.getType()==WayPointType.METRO_TERMINAL||wp.getType()==WayPointType.TERMINAL;
            if (term)
            {
                terminals.add(wp);
                plan.put(wp, new ArrayList<>());
            }
        }

        int itCount=0;

        // MAKE A ROUTE FOR EACH STOP
        for(WayPoint wp: wayPoints)
            if(!terminals.contains(wp))
            {
                boolean valid = false; // start & end are far enough from each other
                List<WayPoint> curTerminals = new ArrayList<>(terminals);

                MatrixElement startME = NearestMapFrom(wp, matrix, curTerminals);
                MatrixElement endME = NearestMap(wp, matrix, curTerminals);
                WayPoint start = startME.getWayPoint();
                WayPoint end = endME.getWayPoint();

                boolean oneMetro = end.getType().equals(WayPointType.METRO_TERMINAL)
                        ||start.getType().equals(WayPointType.METRO_TERMINAL);

                while (!valid&&!curTerminals.isEmpty())
                {
                    if (TimeBetweenMap(start, end, matrix) / 60000 > params.getSiteRadius() &&
                            TimeBetweenMap(end, start, matrix) / 60000 > params.getSiteRadius() &&
                            !start.equals(end) && oneMetro) valid = true;
                    else {
                        if (startME.getTime() > endME.getTime())
                        {
                            curTerminals.remove(start);
                            while(!valid&&!curTerminals.isEmpty())
                            {
                                startME = NearestMapFrom(wp, matrix, curTerminals);
                                start = startME.getWayPoint();
                                oneMetro = end.getType().equals(WayPointType.METRO_TERMINAL)
                                        ||start.getType().equals(WayPointType.METRO_TERMINAL);
                                if (TimeBetweenMap(start, end, matrix) / 60000 > params.getSiteRadius() &&
                                        TimeBetweenMap(end, start, matrix) / 60000 > params.getSiteRadius() &&
                                        !start.equals(end) && oneMetro) valid = true;
                                else
                                    curTerminals.remove(start);
                            }
                        }
                        else
                        {
                            curTerminals.remove(end);
                            while(!valid&&!curTerminals.isEmpty())
                            {
                                endME = NearestMapFrom(wp, matrix, curTerminals);
                                end = endME.getWayPoint();
                                oneMetro = end.getType().equals(WayPointType.METRO_TERMINAL)
                                        ||start.getType().equals(WayPointType.METRO_TERMINAL);
                                if (TimeBetweenMap(start, end, matrix) / 60000 > params.getSiteRadius() &&
                                        TimeBetweenMap(end, start, matrix) / 60000 > params.getSiteRadius() &&
                                        !start.equals(end) && oneMetro) valid = true;
                                else
                                    curTerminals.remove(end);
                            }
                        }
                    }
                }
                // build a route
                Itinerary itinerary = new Itinerary();
                itinerary.setName(start.getDescription() + " - " + end.getDescription());

                itinerary.getWayPointList().add(start);
                itinerary.getWayPointList().add(wp);
                itinerary.getWayPointList().add(end);

                itinerary.setDistance(DistanceBetweenMap(start, wp, matrix) +
                        DistanceBetweenMap(wp, end, matrix));
                itinerary.setTime(TimeBetweenMap(start, wp, matrix) +
                        TimeBetweenMap(wp, end, matrix));


                visitCount.put(start, visitCount.get(start) + 1);
                visitCount.put(wp, visitCount.get(wp) + 1);
                visitCount.put(end, visitCount.get(end) + 1);

                if(!nodes.containsKey(start))
                    nodes.put(start,new TrackList());
                if(!nodes.containsKey(end))
                    nodes.put(end,new TrackList());

                nodes.get(start).getForward().add(itinerary);
                nodes.get(end).getReverse().add(itinerary);


                FillLink(itinerary, 50, false, 0);

                itinerary.setId(itCount++);

                result.getItineraries().add(itinerary);
                result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
                result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
                result.setItineraryQty(result.getItineraryQty() + 1);

            }


        if(params.isLog())
            printKPI(eval(result, matrix), "BEFORE:");


        // DISCARD REDUNDANT
        for(Itinerary ii: new ArrayList<>(result.getItineraries()))
        {
            int redCount = 0;
            for (WayPoint wp : ii.getWayPointList())
                if (visitCount.get(wp) < 2) redCount++;
            if(redCount<params.getRemoveWithLessUnique())
                removeItinerary(ii);
        }

        if(params.isLog())
            printKPI(eval(result, matrix), "AFTER DISCARDING:");

        // LINK INTO ROUTES

        for(WayPoint node: terminals)
        {
            List<WayPoint> terminalsSubset = new ArrayList<>(nodes.keySet());
            if(nodes.containsKey(node))
            while(nodes.get(node).getForward().size()!=nodes.get(node).getReverse().size())
            {
                /*
                System.out.printf("stop %d of %d/%d: %s  out: %d in: %d\n",
                        terminals.indexOf(node),
                        nodes.size(),
                        terminals.size(),
                        node.getDescription(),
                        nodes.get(node).getForward().size(),
                        nodes.get(node).getReverse().size());

                 */

                boolean condition;
                WayPoint start;
                WayPoint end;
                MatrixElement tryME;
                if(nodes.get(node).getForward().size()<nodes.get(node).getReverse().size())
                {
                    start = node;
                    tryME = NearestMap(start, matrix, terminalsSubset);
                    end = tryME.getWayPoint();
                    // add trip if has less incoming
                    condition = nodes.get(end).getForward().size()>nodes.get(end).getReverse().size();

                }
                else
                {
                    end = node;
                    tryME = NearestMapFrom(end, matrix, terminalsSubset);
                    start = tryME.getWayPoint();
                    // add trip if has less outgoing
                    condition = nodes.get(start).getForward().size()<nodes.get(start).getReverse().size();
                }
                if(condition)
                {
                    Itinerary itinerary = new Itinerary();
                    itinerary.setName(end.getDescription() + " - " + start.getDescription());

                    // TODO: map to routes

                    itinerary.getWayPointList().add(start);
                    itinerary.getWayPointList().add(end);

                    itinerary.setDistance(DistanceBetweenMap(start, end, matrix));
                    itinerary.setTime(TimeBetweenMap(start, end, matrix));

                    visitCount.put(start, visitCount.get(start) + 1);
                    visitCount.put(end, visitCount.get(end) + 1);

                    nodes.get(start).getForward().add(itinerary);
                    nodes.get(end).getReverse().add(itinerary);

                    FillLink(itinerary, 50, false, 0);

                    itinerary.setId(itCount++);

                    result.getItineraries().add(itinerary);
                    result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
                    result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
                    result.setItineraryQty(result.getItineraryQty() + 1);
                }
                else
                    terminalsSubset.remove(tryME.getWayPoint());
            }
        }

        if(params.isLog())
            printKPI(eval(result, matrix), "AFTER LINKING:");


        // REMOVE CLOSE STOPS

        for(Itinerary it: result.getItineraries())
        {
            List<WayPoint> toCleanse = new ArrayList<>();
            List<WayPoint> stops = it.getWayPointList();
            for (int i = 0; i < stops.size()-1; i++)
            {
                if(DistanceBetweenMap(stops.get(i),stops.get(i+1),matrix)<params.getMinDistanceBetweenStops())
                    if (i!=0) toCleanse.add(stops.get(i));
                    else toCleanse.add(stops.get(i+1));
            }
            for(WayPoint w: toCleanse)
                stops.remove(w);
        }

        if(params.isLog())
            printKPI(eval(result, matrix), "AFTER CLEANSING:");


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

        // and from nodes!
        WayPoint start = it.getWayPointList().get(0);
        WayPoint end = it.getWayPointList().get(it.getWayPointList().size()-1);

        nodes.get(start).getForward().remove(it);
        nodes.get(end).getReverse().remove(it);

        //System.out.println("-- "+it.getId());
    }
}
