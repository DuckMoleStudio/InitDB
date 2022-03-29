package routing.service.greedyAlgos;

import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.storage.Hop;
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;

import java.time.LocalTime;
import java.util.*;

import static java.lang.Math.round;
import static routing.service.Matrix.DistanceBetweenMap;
import static routing.service.Matrix.TimeBetweenMap;

public class LinkV00 {
    public static Result Calculate(
            List<WayPoint> wayPoints,
            Map<WayPoint, MatrixLineMap> matrix)
    {
        long elTime = System.currentTimeMillis();
        Result result = new Result();
        result.setMethodUsed("Simple Greedy Algorithm, basic link set V.00");
        List<Itinerary> ii = new ArrayList<>();
        result.setItineraries(ii);

        int routeCount = 0;
        List<Hop> routes = new ArrayList<>();

        List<Hop> links = new ArrayList<>();

        Map<WayPoint,List<WayPoint>> plan = new HashMap<>();
        for(WayPoint wp: wayPoints)
            if(wp.getType()==WayPointType.METRO_TERMINAL||wp.getType()==WayPointType.TERMINAL)
            plan.put(wp,new ArrayList<>());

        for(WayPoint wp1: plan.keySet())
            for(WayPoint wp2: plan.keySet())
            {
                double t = TimeBetweenMap(wp1,wp2,matrix)/60000;
                boolean distanceGood = t>10&&t<15;
                boolean clusterAlreadyLinked = false;
                boolean metro = wp1.getType()==WayPointType.METRO_TERMINAL || wp2.getType()==WayPointType.METRO_TERMINAL;

                for(WayPoint wpNear1: plan.keySet())
                    if(TimeBetweenMap(wp1,wpNear1,matrix)/60000<5
                            ||TimeBetweenMap(wpNear1,wp1,matrix)/60000<5
                            ||wp1.equals(wpNear1))
                        for(WayPoint wpNear2: plan.get(wpNear1))
                            if(TimeBetweenMap(wp2,wpNear2,matrix)/60000<5
                                    ||TimeBetweenMap(wpNear2,wp2,matrix)/60000<5
                                    ||wp2.equals(wpNear2))
                                clusterAlreadyLinked=true;

                if (!plan.get(wp1).contains(wp2) && distanceGood && !clusterAlreadyLinked && metro)
                {
                    plan.get(wp1).add(wp2);
                    plan.get(wp2).add(wp1);
                    break;
                }
            }


        for(WayPoint w1: plan.keySet())
            for(WayPoint w2: plan.get(w1))
                links.add(new Hop(w1,w2));

        //System.out.printf("\nFor %d links:\n",links.size());

        // fill with stops
        for(Hop link: links)
        {
            Hop testHop = new Hop(link.getTo(),link.getFrom());
            if(routes.contains(testHop))
              FillLink(link,matrix,result,routes.indexOf(testHop));
            else
            {
                routes.add(link);
                FillLink(link, matrix, result,routes.indexOf(link));
            }
          /*
            System.out.println(link.getFrom().getDescription()+" ----> "
                    +link.getTo().getDescription()+" "
            +link.getTo().getIndex());

           */
        }
        result.setItineraryQty(links.size());

        long elapsedTime = System.currentTimeMillis() - elTime;

        for(Itinerary it: result.getItineraries())
            System.out.println(it.getId()+" "+it.getName());


        System.out.println("\n\nTotal time: " + round(result.getTimeTotal()) / 60000 + " min");
        System.out.println("Total distance: " + round(result.getDistanceTotal()) / 1000 + " km");
        System.out.println("Routes: " + links.size());
        System.out.println("Calculated in: " + elapsedTime + " ms\n");


        return result;
    }


    public static void FillLink(Hop link,
                                Map<WayPoint, MatrixLineMap> matrix,
                                Result result,
                                int routeNo)
    {
        // Itinerary init
        Itinerary itinerary = new Itinerary();
        itinerary.setId(routeNo);
        itinerary.setName(link.getFrom().getDescription()+" - "+link.getTo().getDescription());
        WayPoint curWP = link.getFrom();
        List<WayPoint> ll = new ArrayList<>();
        ll.add(curWP);
        itinerary.setWayPointList(ll);

        List<LocalTime> tt = new ArrayList<>();
        itinerary.setArrivals(tt);
        itinerary.setDistance(0);
        itinerary.setTime(0);

        List<WayPoint> visited = new ArrayList<>();
        visited.add(link.getFrom());
        visited.add(link.getTo());

        TreeMap<Double, Hop> hopMap = new TreeMap<>();
        double totalDistance = DistanceBetweenMap(link.getFrom(),link.getTo(),matrix);
        double totalTime = 0;
        hopMap.put(totalDistance,link);

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

                // if new hops are no longer than old*K
                if((newDistance  < oldDistance*1.01)&&!visited.contains(tryWP))
                {
                    //new 2 hops
                    visited.add(tryWP);
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
        result.getItineraries().add(itinerary);
        result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
        result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
    }


    }
