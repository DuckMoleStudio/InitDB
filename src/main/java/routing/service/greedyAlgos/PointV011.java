package routing.service.greedyAlgos;

import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.eval.CellStopPattern;
import routing.entity.eval.KPIs;
import routing.entity.eval.V01params;
import routing.entity.result.ItPair;
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
    public static Result result, finalResult;
    public static Map<WayPoint,Integer> visitCount = new HashMap<>();
    public static int discardCount;

    public static Map<WayPoint,TrackList> nodes = new HashMap<>();

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
        result.setMethodUsed("Simple Algorithm, linking stops & simple routes V.011");

        discardCount = 0;
        List<WayPoint> terminals = new ArrayList<>();

        // GET TERMINALS
        for(WayPoint wp: wayPoints)
        {
            boolean term;
            if(params.isOnlyMetro())
                term = wp.getType()==WayPointType.METRO_TERMINAL;
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
            printKPI(eval(result, matrix, cellStopPattern), "BEFORE:");


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
            printKPI(eval(result, matrix, cellStopPattern), "AFTER DISCARDING:");

        // GROUP SITES
        for(WayPoint node: terminals)
        if (nodes.containsKey(node))
            {
                // find adjacent
                for(WayPoint tryNear: terminals)
                    if(nodes.containsKey(tryNear)&&
                            (DistanceBetweenMap(node,tryNear,matrix)< params.getSiteRadius()||
                                    DistanceBetweenMap(tryNear,node,matrix)< params.getSiteRadius()))
                    {
                        // reroute to node
                        for(Itinerary out: nodes.get(tryNear).getForward())
                        {
                            out.setName(node.getDescription()+" - "+out.getLast().getDescription());
                            out.setTime(out.getTime()+TimeBetweenMap(node,tryNear,matrix));
                            out.setDistance(out.getDistance()+DistanceBetweenMap(node,tryNear,matrix));
                            out.getWayPointList().add(0,node);

                            nodes.get(node).getForward().add(out);
                        }
                        for(Itinerary in: nodes.get(tryNear).getReverse())
                        {
                            in.setName(in.getFirst().getDescription()+" - "+node.getDescription());
                            in.setTime(in.getTime()+TimeBetweenMap(tryNear,node,matrix));
                            in.setDistance(in.getDistance()+DistanceBetweenMap(tryNear,node,matrix));
                            in.getWayPointList().add(node);

                            nodes.get(node).getReverse().add(in);
                        }
                        // remove from nodes
                        nodes.remove(tryNear);
                    }
            }


        // CREATE ROUTES
        finalResult = new Result();
        finalResult.setMethodUsed("Simple Algorithm, linking stops & simple routes V.011");
        int routeCount = 1;
        int tripCount = 1;

        // 1. Select simple
        for(WayPoint node: terminals)
        {
            if(nodes.containsKey(node))
            {
                List<ItPair> pairs = new ArrayList<>();
                List<Itinerary> revUsed = new ArrayList<>();
                for(Itinerary forward: nodes.get(node).getForward())
                    for(Itinerary reverse: nodes.get(node).getReverse())
                        if(reverse.getFirst().equals(forward.getLast()) && !revUsed.contains(reverse))
                        {
                            pairs.add(new ItPair(forward, reverse));
                            revUsed.add(reverse);
                            break;
                        }

                // add & remove
                for(ItPair pair: pairs)
                {
                    WayPoint start = node;
                    WayPoint end = pair.getForward().getLast();

                    pair.getForward().setId(tripCount++);
                    pair.getForward().setRoute(routeCount);
                    pair.getForward().setDir(0);
                    pair.getReverse().setId(tripCount++);
                    pair.getReverse().setRoute(routeCount);
                    pair.getReverse().setDir(1);
                    routeCount++;

                    // and from nodes!

                    nodes.get(start).getForward().remove(pair.getForward());
                    nodes.get(start).getReverse().remove(pair.getReverse());

                    nodes.get(end).getForward().remove(pair.getReverse());
                    nodes.get(end).getReverse().remove(pair.getForward());

                    finalResult.getItineraries().add(pair.getForward());
                    finalResult.getItineraries().add(pair.getReverse());
                    finalResult.setDistanceTotal(finalResult.getDistanceTotal()
                            + pair.getForward().getDistance() + pair.getReverse().getDistance());
                    finalResult.setTimeTotal(finalResult.getTimeTotal()
                            + pair.getForward().getTime() + pair.getReverse().getTime());
                    finalResult.setItineraryQty(finalResult.getItineraryQty() + 2);
                }
            }
        }

        // 2. Select circular
        for(WayPoint node: terminals)
        {
            if(nodes.containsKey(node))
            {
                for(Itinerary out: new ArrayList<>(nodes.get(node).getForward()))
                {
                    Itinerary circle = out.withName(node.getDescription()+" - "+node.getDescription());
                    circle.getWayPointList().add(node);
                    double min = TimeBetweenMap(out.getLast(),out.getFirst(),matrix);
                    circle.setTime(circle.getTime()+min);
                    circle.setDistance(circle.getDistance()+DistanceBetweenMap(out.getLast(),out.getFirst(),matrix));

                    nodes.get(node).getForward().remove(out);
                    nodes.get(out.getLast()).getReverse().remove(out);


                            circle.setId(tripCount++);
                            circle.setRoute(routeCount++);
                            circle.setDir(0);

                    FillLink(circle, 50, false, 0);


                            finalResult.getItineraries().add(circle);
                            finalResult.setDistanceTotal(finalResult.getDistanceTotal() + circle.getDistance());
                            finalResult.setTimeTotal(finalResult.getTimeTotal() + circle.getTime());
                            finalResult.setItineraryQty(finalResult.getItineraryQty() + 1);
                        }
                    }

            }




        // REMOVE CLOSE STOPS

        for(Itinerary it: finalResult.getItineraries())
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
            printKPI(eval(finalResult, matrix, cellStopPattern), "AFTER CLEANSING:");


        return finalResult;
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

        nodes.get(it.getFirst()).getForward().remove(it);
        nodes.get(it.getLast()).getReverse().remove(it);

        //System.out.println("-- "+it.getId());
    }

}
