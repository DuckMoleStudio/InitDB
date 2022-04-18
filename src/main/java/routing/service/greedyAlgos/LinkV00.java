package routing.service.greedyAlgos;

import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.eval.KPIs;
import routing.entity.eval.V00params;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.storage.Hop;
import routing.entity.storage.MatrixElement;
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;

import java.time.LocalTime;
import java.util.*;

import static java.lang.Math.round;
import static routing.service.Evaluate.eval;
import static routing.service.Matrix.*;

public class LinkV00 {

    public static Map<WayPoint, MatrixLineMap> matrix;
    public static Result result;
    public static Map<WayPoint,Integer> visitCount = new HashMap<>();

    public static Result Calculate(
            List<WayPoint> wayPoints,
            Map<WayPoint, MatrixLineMap> matrixIn,
            V00params params)
    {
        matrix = matrixIn;
        for(WayPoint wp: wayPoints)
            visitCount.put(wp,0);

        result = new Result();
        result.setMethodUsed("Simple Behaviourist Algorithm, linking terminals V.00");

        int discardCount = 0;
        List<Hop> routes = new ArrayList<>(); // index is route number

        List<Hop> links = new ArrayList<>();

        // GET TERMINALS
        Map<WayPoint,List<WayPoint>> plan = new HashMap<>();
        for(WayPoint wp: wayPoints)
            if(wp.getType()==WayPointType.METRO_TERMINAL||wp.getType()==WayPointType.TERMINAL)
            plan.put(wp,new ArrayList<>());

        // CREATE LINKS
        for(WayPoint wp1: plan.keySet())
            for(WayPoint wp2: plan.keySet())
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


        for(WayPoint w1: plan.keySet())
            for(WayPoint w2: plan.get(w1))
                links.add(new Hop(w1,w2));

        //System.out.printf("\nFor %d links:\n",links.size());

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
                itinerary.setId(routes.indexOf(reverseLink)); // if we have reverse, use its number
            else
            {
                routes.add(link);
                itinerary.setId(routes.indexOf(link)); // new number
            }

            visitCount.put(link.getFrom(),visitCount.get(link.getFrom())+1);
            visitCount.put(link.getTo(),visitCount.get(link.getTo())+1);
            FillLink(itinerary,50,false,0);

            result.getItineraries().add(itinerary);
            result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
            result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
        }

        result.setItineraryQty(links.size());

        KPIs kpis = eval(result, matrix);

        System.out.println("BEFORE:\n");
        System.out.println("KPI #1: " + kpis.getCellToStop());
        System.out.println("KPI #2: " + kpis.getCellToMetroSimple());
        System.out.println("KPI #3: " + kpis.getCellToMetroFull());
        System.out.println(kpis.getRouteCount() + " trips");
        System.out.println(kpis.getStopCount() + " stops used");
        System.out.println("total distance: " + kpis.getTotalDistance() / 1000);

        // GET UNUSED & MAP TO LINKS
        Map<Hop,List<WayPoint>> unused = new HashMap<>();
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
                if(unused.containsKey(nearestHop))
                    unused.get(nearestHop).add(wp);
                else
                    unused.put(nearestHop,new ArrayList<>(List.of(wp)));
            }

for(Hop h: routes)
    if(unused.containsKey(h))
    System.out.printf("# %d ----> %d unused stops\n",routes.indexOf(h),unused.get(h).size());




        // BUILD WITH UNUSED

        for(Hop link: unused.keySet())
        {
            Hop reverseLink = new Hop(link.getTo(),link.getFrom()); // reverse link
            // Itinerary init
            Itinerary itinerary = new Itinerary();
            itinerary.setName(link.getFrom().getDescription()+" - "+link.getTo().getDescription());

            itinerary.getWayPointList().add(link.getFrom());


            itinerary.setDistance(0);
            itinerary.setTime(0);

            if(routes.contains(link))
                itinerary.setId(routes.indexOf(link)*10+1);
            else
                itinerary.setId(routes.indexOf(reverseLink)*10+1);

            visitCount.put(link.getFrom(),visitCount.get(link.getFrom())+1);
            visitCount.put(link.getTo(),visitCount.get(link.getTo())+1);

            // very simple greedy fill
            List<WayPoint> wpList = new ArrayList<>(unused.get(link));
            WayPoint curWP = link.getFrom();
            while (!wpList.isEmpty())
            {
                MatrixElement me = NearestMap(curWP, matrix, wpList);
                WayPoint tryWP = me.getWayPoint();

                // visit this WP
                visitCount.put(tryWP,visitCount.get(tryWP)+1);
                itinerary.getWayPointList().add(tryWP);
                itinerary.setDistance(itinerary.getDistance() + me.getDistance());
                itinerary.setTime(itinerary.getTime() + me.getTime());

                curWP = tryWP;
                wpList.remove(tryWP);
            }

            itinerary.getWayPointList().add(link.getTo());

            // now fill it
            //FillLink(itinerary,1.01,false,0);

            result.getItineraries().add(itinerary);
            result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
            result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());

            result.setItineraryQty(result.getItineraryQty()+1);
        }

        kpis = eval(result, matrix);

        System.out.println("\nWITH UNUSED:\n");
        System.out.println("KPI #1: " + kpis.getCellToStop());
        System.out.println("KPI #2: " + kpis.getCellToMetroSimple());
        System.out.println("KPI #3: " + kpis.getCellToMetroFull());
        System.out.println(kpis.getRouteCount() + " trips");
        System.out.println(kpis.getStopCount() + " stops used");
        System.out.println("total distance: " + kpis.getTotalDistance() / 1000);

        // DISCARD REDUNDANT

        for(Itinerary it: new ArrayList<>(result.getItineraries()))
        {
            int redCount = 0;
            for(WayPoint wp: it.getWayPointList())
                if(visitCount.get(wp)<2) redCount++;

           // System.out.printf("total: %d ----> unique: %d\n", it.getWayPointList().size(),redCount);

            if(redCount<1) // control parameter
            {
                for(WayPoint wp: it.getWayPointList())
                    visitCount.put(wp,visitCount.get(wp)-1);
                result.getItineraries().remove(it);
                result.setDistanceTotal(result.getDistanceTotal() - it.getDistance());
                result.setTimeTotal(result.getTimeTotal() - it.getTime());
                result.setItineraryQty(result.getItineraryQty()-1);
                discardCount++;

                System.out.println("-- "+it.getId());
            }
        }

        System.out.println("\nDiscarded: "+discardCount);

        kpis = eval(result, matrix);

        System.out.println("\nAFTER DISCARDING:\n");
        System.out.println("KPI #1: " + kpis.getCellToStop());
        System.out.println("KPI #2: " + kpis.getCellToMetroSimple());
        System.out.println("KPI #3: " + kpis.getCellToMetroFull());
        System.out.println(kpis.getRouteCount() + " trips");
        System.out.println(kpis.getStopCount() + " stops used");
        System.out.println("total distance: " + kpis.getTotalDistance() / 1000);



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


    }
