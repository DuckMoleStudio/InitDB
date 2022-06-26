package routing.service.algos;

import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.eval.AlgoParams;
import routing.entity.eval.CellStopPattern;
import routing.entity.eval.KPIs;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.result.TrackList;
import routing.entity.storage.Hop;
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;

import java.util.*;

import static routing.service.Evaluate.eval;
import static routing.service.Matrix.*;

public class LinkV00 {

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
        result.setMethodUsed("Simple Behaviour Algorithm, linking terminals V.00");

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
            if(wp.getType()==WayPointType.METRO_TERMINAL||wp.getType()==WayPointType.TERMINAL)
            {
                terminals.add(wp);
                plan.put(wp, new ArrayList<>());
            }

        // CREATE LINKS
        for(WayPoint wp1: terminals)
            for(WayPoint wp2: terminals)
            {
                double t = TimeBetweenMap(wp1,wp2,matrix)/60000;
                boolean distanceGood = t>params.getMinDistance()&&t<params.getMaxDistance();
                boolean clusterAlreadyLinked = false;
                boolean metro = wp1.getType()==WayPointType.METRO_TERMINAL || wp2.getType()==WayPointType.METRO_TERMINAL;

                for(WayPoint wpNear1: plan.keySet())
                    if(TimeBetweenMap(wp1,wpNear1,matrix)/60000<params.getSiteRadius()
                            ||TimeBetweenMap(wpNear1,wp1,matrix)/60000<params.getSiteRadius()
                            ||wp1.equals(wpNear1))
                        for(WayPoint wpNear2: plan.get(wpNear1))
                            if(TimeBetweenMap(wp2,wpNear2,matrix)/60000<params.getSiteRadius()
                                    ||TimeBetweenMap(wpNear2,wp2,matrix)/60000<params.getSiteRadius()
                                    ||wp2.equals(wpNear2))
                                clusterAlreadyLinked=true;

                if (!plan.get(wp1).contains(wp2) && distanceGood && !clusterAlreadyLinked && metro)
                {
                    plan.get(wp1).add(wp2);
                    plan.get(wp2).add(wp1);
                    break;
                }
            }


        for(WayPoint w1: terminals)
            for(WayPoint w2: plan.get(w1))
                links.add(new Hop(w1,w2));

        // FILL WITH STOPS

        for(Hop link: links)
        {
            Hop reverseLink = new Hop(link.getTo(),link.getFrom()); // reverse link

            // Itinerary init
            Itinerary itinerary = new Itinerary();
            itinerary.setName(link.getFrom().getDescription()+" - "+link.getTo().getDescription());

            itinerary.getWayPointList().add(link.getFrom());
            itinerary.getWayPointList().add(link.getTo());

            itinerary.setDistance(0);
            itinerary.setTime(0);

            if(routes.contains(reverseLink))
            {
                itinerary.setRoute(routes.indexOf(reverseLink)+1); // if we have reverse, use its number
                itinerary.setId(1);
                routeR.put(reverseLink,1);
                itinerary.setDir(1);
                trips.get(reverseLink).getReverse().add(itinerary);
            }
            else
            {
                routes.add(link);
                itinerary.setRoute(routes.indexOf(link)+1); // new number
                itinerary.setId(1);
                routeF.put(link,1);
                itinerary.setDir(0);
                trips.put(link,new TrackList());
                trips.get(link).getForward().add(itinerary);
            }

            visitCount.put(link.getFrom(),visitCount.get(link.getFrom())+1);
            visitCount.put(link.getTo(),visitCount.get(link.getTo())+1);
            FillLink(itinerary,50,false,0);

            result.getItineraries().add(itinerary);
            result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
            result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
        }

        result.setItineraryQty(links.size());

        if(params.isLog())
        printKPI(eval(result, matrix, cellStopPattern), "BEFORE:");

        // GET UNUSED & MAP TO LINKS
        Map<Hop,List<WayPoint>> unusedMap = new HashMap<>();
        List<Hop> reused = new ArrayList<>();
        for(WayPoint wp: wayPoints)
            if(visitCount.get(wp)==0)
            {
                double minDistance = Double.POSITIVE_INFINITY;
                Hop nearestHop = new Hop();
                for(Hop link: links)
                {
                    double biDistance = DistanceBetweenMap(link.getFrom(),link.getTo(),matrixIn);
                    double triDistance = DistanceBetweenMap(link.getFrom(),wp,matrixIn) +
                            DistanceBetweenMap(wp,link.getTo(),matrixIn);
                    double delta = triDistance - biDistance;
                    if(delta<minDistance)
                    {
                        minDistance = delta;
                        nearestHop = link;
                    }
                }
                if(unusedMap.containsKey(nearestHop))
                    unusedMap.get(nearestHop).add(wp);
                else
                {
                    reused.add(nearestHop);
                    unusedMap.put(nearestHop, new ArrayList<>(List.of(wp)));
                }
            }



        // BUILD WITH UNUSED

        for(Hop link: reused)
        for(WayPoint wp: unusedMap.get(link))
        {
            Hop reverseLink = new Hop(link.getTo(),link.getFrom()); // reverse link
            // Itinerary init
            Itinerary itinerary = new Itinerary();
            itinerary.setName(link.getFrom().getDescription()+" - "+link.getTo().getDescription());

            itinerary.getWayPointList().add(link.getFrom());
            itinerary.getWayPointList().add(wp);
            itinerary.getWayPointList().add(link.getTo());

            itinerary.setDistance(DistanceBetweenMap(link.getFrom(),wp,matrix)+
                    DistanceBetweenMap(wp,link.getTo(),matrix));
            itinerary.setTime(TimeBetweenMap(link.getFrom(),wp,matrix)+
                    TimeBetweenMap(wp,link.getTo(),matrix));


            visitCount.put(link.getFrom(),visitCount.get(link.getFrom())+1);
            visitCount.put(wp,visitCount.get(wp)+1);
            visitCount.put(link.getTo(),visitCount.get(link.getTo())+1);

            FillLink(itinerary,50,true,0);

            if(itinerary.getWayPointList().size()>(params.getAddNoLessNewStops()+2)) // control parameter, +2 terminals
            {
                FillLink(itinerary,50,false,0);
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
                result.getItineraries().add(itinerary);
                result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
                result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
                result.setItineraryQty(result.getItineraryQty() + 1);
            }
            else
                for(WayPoint w: itinerary.getWayPointList())
                    visitCount.put(w,visitCount.get(w)-1);
        }

        if(params.isLog())
        printKPI(eval(result, matrix, cellStopPattern), "WITH UNUSED:");


        // DISCARD REDUNDANT

        for(Hop link: routes)
        {
            int countForward = 0;
            int countReverse = 0;
            TreeMap<Integer, List<Itinerary>> forward = new TreeMap<>();
            TreeMap<Integer, List<Itinerary>> reverse = new TreeMap<>();

            //System.out.printf("Route %d: %d forward, %d reverse ",routes.indexOf(link)+1,trips.get(link).getForward().size(),trips.get(link).getReverse().size());

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
            for (Itinerary ii : trips.get(link).getReverse()) {
                int redCount = 0;
                for (WayPoint wp : ii.getWayPointList())
                    if (visitCount.get(wp) < 2) redCount++;

                if(reverse.containsKey(redCount))
                    reverse.get(redCount).add(ii);
                else
                    reverse.put(redCount, new ArrayList<>(List.of(ii)));
            }
            if (reverse.lastKey() < params.getRemoveWithLessUnique() && forward.lastKey() < params.getRemoveWithLessUnique()) {
                // remove all
                for(Map.Entry<Integer,List<Itinerary>> me: forward.entrySet())
                for(Itinerary iii: me.getValue()) removeItinerary(iii);
                for(Map.Entry<Integer,List<Itinerary>> me: reverse.entrySet())
                for(Itinerary iii: me.getValue()) removeItinerary(iii);
                //System.out.println(" removed all");
                continue;
            }

            if(forward.lastKey() < params.getRemoveWithLessUnique() || params.isOnePair())
            {
                // remove all forward but last
                for(Map.Entry<Integer,List<Itinerary>> me: forward.entrySet())
                    for(Itinerary iii: me.getValue())
                        if(me.getKey().intValue()==forward.lastKey().intValue())
                        {
                            //System.out.print("-");
                            if(me.getValue().size()>1&&me.getValue().indexOf(iii)!=0)
                            {removeItinerary(iii); countForward++;
                                //System.out.print(".");
                            }
                        }
                else {removeItinerary(iii); countForward++;
                    //System.out.print(":");
                }
            }
            else
            {
                // remove all forward by criteria
                for(Map.Entry<Integer,List<Itinerary>> me: forward.entrySet())
                    for(Itinerary iii: me.getValue())
                        if(me.getKey()<params.getRemoveWithLessUnique()) {removeItinerary(iii); countForward++;}
            }

            if(reverse.lastKey() < params.getRemoveWithLessUnique() || params.isOnePair())
            {
                // remove all reverse but last
                for(Map.Entry<Integer,List<Itinerary>> me: reverse.entrySet())
                    for(Itinerary iii: me.getValue())
                        if(me.getKey().intValue()==reverse.lastKey().intValue())
                        {
                            //System.out.print("-");
                            if(me.getValue().size()>1&&me.getValue().indexOf(iii)!=0)
                            {removeItinerary(iii); countReverse++;
                                //System.out.print(".");
                            }
                        }
                        else {removeItinerary(iii); countReverse++;
                            //System.out.print(":");
                        }
            }
            else
            {
                // remove all reverse by criteria
                for(Map.Entry<Integer,List<Itinerary>> me: reverse.entrySet())
                    for(Itinerary iii: me.getValue())
                        if(me.getKey()<params.getRemoveWithLessUnique()) {removeItinerary(iii); countReverse++;}
            }

            //System.out.printf("removed %d - %d\n",countForward,countReverse);
        }

        //System.out.println("\nDiscarded: "+discardCount);

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
