package routing.service.greedyAlgos;

import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.eval.CellStopPattern;
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

public class PointV01 {


    public static Map<WayPoint, MatrixLineMap> matrix;
    public static Result result;
    public static Map<WayPoint,Integer> visitCount = new HashMap<>();
    public static int discardCount;

    public static Result Calculate(
            List<WayPoint> wayPoints,
            Map<WayPoint, MatrixLineMap> matrixIn,
            V01params params,
            CellStopPattern cellStopPattern)
    {
        matrix = matrixIn;
        for(WayPoint wp: wayPoints)
            visitCount.put(wp,0);

        result = new Result();
        result.setMethodUsed("Simple Behaviour Algorithm, linking stops V.01");

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
                    if (TimeBetweenMap(start, end, matrix) / 60000 > params.getMinTerminalGap() &&
                            TimeBetweenMap(end, start, matrix) / 60000 > params.getMinTerminalGap() &&
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
                                if (TimeBetweenMap(start, end, matrix) / 60000 > params.getMinTerminalGap() &&
                                        TimeBetweenMap(end, start, matrix) / 60000 > params.getMinTerminalGap() &&
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
                                if (TimeBetweenMap(start, end, matrix) / 60000 > params.getMinTerminalGap() &&
                                        TimeBetweenMap(end, start, matrix) / 60000 > params.getMinTerminalGap() &&
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

                // map to routes
                Hop link = new Hop(start,end);
                Hop reverseLink = new Hop(end,start); // reverse link
                if(routes.contains(link)||routes.contains(reverseLink))
                {
                    if(routes.contains(link))
                    {
                        itinerary.setRoute(routes.indexOf(link)+1); // forward number
                        itinerary.setId(routeF.get(link)+1);
                        routeF.put(link,routeF.get(link)+1);
                        itinerary.setDir(0);
                        trips.get(link).getForward().add(itinerary);
                    }
                    else
                    {
                        itinerary.setRoute(routes.indexOf(reverseLink)+1); // if we have reverse, use its number
                        itinerary.setId(routeR.get(reverseLink)+1);
                        routeR.put(reverseLink,routeR.get(reverseLink)+1);
                        itinerary.setDir(1);
                        trips.get(reverseLink).getReverse().add(itinerary);
                    }
                }
                else
                {
                    routes.add(link);
                    itinerary.setRoute(routes.indexOf(link)+1); // new number
                    itinerary.setId(1);
                    routeF.put(link,1);
                    routeR.put(link,0);
                    itinerary.setDir(0);
                    trips.put(link,new TrackList());
                    trips.get(link).getForward().add(itinerary);
                }



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

                FillLink(itinerary, 50, false, 0);

                itinerary.setId(itCount++);

                    result.getItineraries().add(itinerary);
                    result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
                    result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
                    result.setItineraryQty(result.getItineraryQty() + 1);

            }


        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "BEFORE:");


        // DISCARD REDUNDANT
        for(Hop link: routes)
        {
            TreeMap<Integer, List<Itinerary>> forward = new TreeMap<>();
            TreeMap<Integer, List<Itinerary>> reverse = new TreeMap<>();

            for (Itinerary ii : trips.get(link).getForward())
            {
                int redCount = 0;
                for (WayPoint wp : ii.getWayPointList())
                    if (visitCount.get(wp) < 2) redCount++;

                if(forward.containsKey(redCount))
                    forward.get(redCount).add(ii);
                else
                    forward.put(redCount, new ArrayList<>(List.of(ii)));
            }

            if(trips.get(link).getReverse().isEmpty())
            {
                // force 1 reverse trip
                WayPoint start = link.getTo();
                WayPoint end = link.getFrom();
                Itinerary itinerary = new Itinerary();
                itinerary.setName(end.getDescription() + " - " + start.getDescription());

                // map to routes
                itinerary.setRoute(routes.indexOf(link)+1); // forward number
                itinerary.setId(1);
                routeR.put(link,1);
                itinerary.setDir(1);
                trips.get(link).getReverse().add(itinerary);

                itinerary.getWayPointList().add(start);
                itinerary.getWayPointList().add(end);

                itinerary.setDistance(DistanceBetweenMap(start, end, matrix));
                itinerary.setTime(TimeBetweenMap(start, end, matrix));


                visitCount.put(start, visitCount.get(start) + 1);
                visitCount.put(end, visitCount.get(end) + 1);

                FillLink(itinerary, 50, false, 0);

                itinerary.setId(itCount++);

                result.getItineraries().add(itinerary);
                result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
                result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
                result.setItineraryQty(result.getItineraryQty() + 1);
            }

            for (Itinerary ii : trips.get(link).getReverse())
            {
                int redCount = 0;
                for (WayPoint wp : ii.getWayPointList())
                    if (visitCount.get(wp) < 2) redCount++;

                if(reverse.containsKey(redCount))
                    reverse.get(redCount).add(ii);
                else
                    reverse.put(redCount, new ArrayList<>(List.of(ii)));
            }



            if (reverse.lastKey() < params.getRemoveWithLessUnique() && forward.lastKey() < params.getRemoveWithLessUnique())
            {
                // remove all
                for(Map.Entry<Integer,List<Itinerary>> me: forward.entrySet())
                    for(Itinerary iii: me.getValue()) removeItinerary(iii);
                for(Map.Entry<Integer,List<Itinerary>> me: reverse.entrySet())
                    for(Itinerary iii: me.getValue()) removeItinerary(iii);
                continue;
            }

            if(forward.lastKey() < params.getRemoveWithLessUnique() || params.isOnePair())
            {
                // remove all forward but last
                for(Map.Entry<Integer,List<Itinerary>> me: forward.entrySet())
                    for(Itinerary iii: me.getValue())
                        if(me.getKey().intValue()==forward.lastKey().intValue())
                        {
                            if(me.getValue().size()>1&&me.getValue().indexOf(iii)!=0)
                            removeItinerary(iii);
                        }
                        else removeItinerary(iii);
            }
            else
            {
                // remove all forward by criteria
                for(Map.Entry<Integer,List<Itinerary>> me: forward.entrySet())
                    for(Itinerary iii: me.getValue())
                        if(me.getKey()<params.getRemoveWithLessUnique()) removeItinerary(iii);
            }

            if(reverse.lastKey() < params.getRemoveWithLessUnique() || params.isOnePair())
            {
                // remove all reverse but last
                for(Map.Entry<Integer,List<Itinerary>> me: reverse.entrySet())
                    for(Itinerary iii: me.getValue())
                        if(me.getKey().intValue()==reverse.lastKey().intValue())
                        {
                            if(me.getValue().size()>1&&me.getValue().indexOf(iii)!=0)
                            removeItinerary(iii);
                        }
                        else removeItinerary(iii);
            }
            else
            {
                // remove all reverse by criteria
                for(Map.Entry<Integer,List<Itinerary>> me: reverse.entrySet())
                    for(Itinerary iii: me.getValue())
                        if(me.getKey()<params.getRemoveWithLessUnique()) removeItinerary(iii);
            }
        }

        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "AFTER DISCARDING:");


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
            printKPI(eval(result, matrix, cellStopPattern), "AFTER CLEANSING:");


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
