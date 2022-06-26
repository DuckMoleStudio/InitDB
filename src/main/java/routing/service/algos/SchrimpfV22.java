package routing.service.algos;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.util.Solutions;
import com.graphhopper.jsprit.core.util.VehicleRoutingTransportCostsMatrix;
import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.eval.AlgoParams;
import routing.entity.eval.CellStopPattern;
import routing.entity.eval.KPIs;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.result.TrackList;
import routing.entity.storage.Hop;
import routing.entity.storage.MatrixElement;
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;
import routing.service.Matrix;

import java.util.*;

import static routing.service.Evaluate.eval;
import static routing.service.Matrix.*;

public class SchrimpfV22 {

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
        result.setMethodUsed("Jsprit all in range to all V22");

        discardCount = 0;



        // ---- prepare point sets ----



        List<WayPoint> metroOnly = new ArrayList<>();
        List<WayPoint> stopsOnly = new ArrayList<>();
        List<WayPoint> terminals = new ArrayList<>();
        List<WayPoint> allMetro = new ArrayList<>();

        for(WayPoint wp: wayPoints)
        {
            if (wp.getType() == WayPointType.TERMINAL || wp.getType() == WayPointType.METRO_TERMINAL)
                terminals.add(wp);
            if (wp.getType() == WayPointType.METRO_TERMINAL)
                metroOnly.add(wp);
            if (wp.getType() == WayPointType.STOP)
                stopsOnly.add(wp);
            if (wp.getType() == WayPointType.METRO_TERMINAL || wp.getType() == WayPointType.METRO)
                allMetro.add(wp);
        }

        Map<WayPoint,Set<WayPoint>> clusters = new HashMap<>();
        for(WayPoint wp: metroOnly)
        {
            Set<WayPoint> cluster;
            if(!clusters.containsKey(wp))
            {
                cluster = new HashSet<>(List.of(wp));
                clusters.put(wp, cluster);
            }
            else
                cluster = clusters.get(wp);
            for(WayPoint wpTry: metroOnly)
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

        Set<Set<WayPoint>> clusterSet = new HashSet<>(clusters.values());

        System.out.printf("\n%d clusters total for %d metro", clusterSet.size(),metroOnly.size());

        for (Set<WayPoint> cluster: clusterSet)
        {
            System.out.println("\n\nCluster:");
            for(WayPoint wp: cluster)
                System.out.printf("%s (%d)\n",wp.getDescription(),wp.getIndex());
        }

        Map<WayPoint,List<WayPoint>> toMetro = new HashMap<>();
        List<WayPoint> metro4Unused = new ArrayList<>();
        List<WayPoint> unusedToTerminal = new ArrayList<>();

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
                metro4Unused.add(metro);
        }





        for(WayPoint metro: metro4Unused)
        {
            toMetro.put(metro, new ArrayList<>());
            for (WayPoint wp : stopsOnly)
            {
                double timeTo = TimeBetweenMap(wp, metro, matrix);
                double timeFrom = TimeBetweenMap(metro, wp, matrix);
                if (timeTo < params.getMaxDistance()*60000 && timeTo < timeFrom)
                    toMetro.get(metro).add(wp);

            }
        }

        // all jsprit starts here
        //define a matrix-builder building a NON-symmetric matrix
        VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix
                .Builder.newInstance(false);

        System.out.print("\nBuilding matrix...");

        // ---- transfer our matrix to jsprit matrix ----
        for(int jj=0;jj<wayPoints.size();jj++)
            for(int kk=0;kk<wayPoints.size();kk++)
                if(jj!=kk)
                {
                    costMatrixBuilder.addTransportDistance(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Matrix.DistanceBetweenMap(wayPoints.get(jj),wayPoints.get(kk),matrix));
                    costMatrixBuilder.addTransportTime(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Matrix.TimeBetweenMap(wayPoints.get(jj),wayPoints.get(kk),matrix));
                }
                else
                {
                    costMatrixBuilder.addTransportDistance(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Double.POSITIVE_INFINITY);
                    costMatrixBuilder.addTransportTime(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Double.POSITIVE_INFINITY);
                }
        VehicleRoutingTransportCosts costMatrix = costMatrixBuilder.build();

        System.out.println("  done!\n");

        /*
         * get a vehicle type-builder and build a type
         */
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("Moscow Bus Type")
                .addCapacityDimension(0, params.getCapacity());
        VehicleType vehicleTypeMoscowBus = vehicleTypeBuilder.build();

        int routeCount = 1;
        int itCount = 1;

        // now cycle with jsprit 4 every metro
        for(WayPoint metro: metro4Unused)
        {
//WayPoint metro = metroOnly.get(0);
            List<Shipment> shipments = new ArrayList<>();
            Set<WayPoint> subTerminals = new HashSet<>();

            double maxHop = params.getMaxDistance()*60000*1.5; // <----------- SET K HERE

            for(WayPoint c_wp: toMetro.get(metro))
            {
                double timeTo = TimeBetweenMap(c_wp, metro, matrix);
                if(timeTo > params.getMinDistance()*60000)
                    subTerminals.add(c_wp);
            }

            for(WayPoint c_wp: toMetro.get(metro))
            {
                double hop = TimeBetweenMap(c_wp,metro,matrix);
                shipments.add(Shipment.Builder
                        .newInstance(String.valueOf(wayPoints.indexOf(c_wp)))
                        .addSizeDimension(0, 1)
                        .setPickupLocation(Location.newInstance(String.valueOf(wayPoints.indexOf(c_wp))))
                        .setDeliveryLocation(Location.newInstance
                                (String.valueOf(wayPoints.indexOf(metro))))
                        .setPickupServiceTime(1)
                        .setDeliveryServiceTime(1)
                        .setDeliveryTimeWindow(TimeWindow.newInstance(0,maxHop))
                        .build());
            }

            if(subTerminals.size()==0) subTerminals.add(metro);

            System.out.printf("\nSelected %d terminals and %d stops for: %s (%d)\n",
                    subTerminals.size(), toMetro.get(metro).size(), metro.getDescription(), metro.getIndex());


            //get a vehicle-builder and build vehicles

            List<VehicleImpl> vehicles = new ArrayList<>();

            int cc = 0;

            for (WayPoint wp : subTerminals)
            {
                String routeName = wp.getDescription() + " " + cc++;
                VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(routeName);
                String start = String.valueOf(wayPoints.indexOf(wp));
                vehicleBuilder.setStartLocation(Location.newInstance(start));
                vehicleBuilder.setLatestArrival(maxHop);
                vehicleBuilder.setType(vehicleTypeMoscowBus);
                vehicles.add(vehicleBuilder.build());
            }


            // --- set up routing problem
            VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance()
                    .setFleetSize(VehicleRoutingProblem.FleetSize.INFINITE)
                    .setRoutingCost(costMatrix);

            // ----- add cars and services (points) to the problem

            for (VehicleImpl vehicle : vehicles)
                vrpBuilder.addVehicle(vehicle);
            for (Shipment ss : shipments)
                vrpBuilder.addJob(ss);


            VehicleRoutingProblem problem = vrpBuilder.build();

            VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
                    .setProperty(Jsprit.Parameter.THREADS, "4")
                    .buildAlgorithm();
            algorithm.setMaxIterations(params.getIterations());

            //and search a solution

            Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

            //get the best

            VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

            //SolutionPrinter.print(problem, bestSolution, SolutionPrinter.Print.VERBOSE);

            // --- parse solutions to itineraries
            List<Itinerary> itinerariesJS = new ArrayList<>();

            List<VehicleRoute> routesJS = new ArrayList<VehicleRoute>(bestSolution.getRoutes());
            Collections.sort(routesJS, new com.graphhopper.jsprit.core.util.VehicleIndexComparator());

            for (VehicleRoute route : routesJS) {
                Itinerary curItinerary = new Itinerary();

                TourActivity prevAct = route.getStart();
                String jobId;
                jobId = prevAct.getLocation().getId();
                curItinerary.getWayPointList().add(wayPoints.get(Integer.parseInt(jobId)));
                String prevName = "";

                for (TourActivity act : route.getActivities())
                {
                    if (act instanceof TourActivity.JobActivity)
                    {
                        // main writing
                        if (act.getName().equals("deliverShipment")) {
                            if (!prevName.equals("deliverShipment")) {
                                curItinerary.getWayPointList().add(metro);
                                prevName = "deliverShipment";
                            }
                        } else {
                            jobId = ((TourActivity.JobActivity) act).getJob().getId();
                            curItinerary.getWayPointList().add(wayPoints.get(Integer.parseInt(jobId)));
                            prevName = act.getName();
                        }
                    }
                }
                itinerariesJS.add(curItinerary);
            }

            // COMPLETE ITINERARIES


            List<WayPoint> terminalsMetro = new ArrayList<>();
            for (WayPoint wp : wayPoints)
                if (wp.getType() == WayPointType.METRO_TERMINAL)
                    terminalsMetro.add(wp);

            for (Itinerary itinerary : itinerariesJS)
            {
                WayPoint start = itinerary.getFirst();
                WayPoint end = itinerary.getLast();
                itinerary.setName("***  " + start.getDescription() + " - " + end.getDescription());

/*
                for (WayPoint wp : itinerary.getWayPointList()) {
                    visitCount.put(wp, visitCount.get(wp) + 1);
                    if (!wp.equals(start)) {
                        itinerary.setDistance(itinerary.getDistance()
                                + DistanceBetweenMap(
                                itinerary.getWayPointList().get(itinerary.getWayPointList().indexOf(wp) - 1),
                                wp, matrix));
                        itinerary.setTime(itinerary.getTime()
                                + TimeBetweenMap(
                                itinerary.getWayPointList().get(itinerary.getWayPointList().indexOf(wp) - 1),
                                wp, matrix));
                    }
                }

 */

                if(start.equals(itinerary.getWayPointList().get(1)))
                    itinerary.getWayPointList().remove(1);
                if(!end.equals(start))
                {
                    itinerary.getWayPointList().add(start);
                    itinerary.setDistance(itinerary.getDistance() +
                            DistanceBetweenMap(end, start, matrix));
                    itinerary.setTime(itinerary.getTime() +
                            TimeBetweenMap(end, start, matrix));
                }

                FillLink(itinerary, 50, false, 0);

                itinerary.setId(itCount++);
                itinerary.setRoute(routeCount++);
                itinerary.setDir(0);

                result.getItineraries().add(itinerary);
                result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
                result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
                result.setItineraryQty(result.getItineraryQty() + 1);
            }

        }

        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "WITH UNUSED:");


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
        double totalTime = 0;
        double totalDistance = 0;

        TreeMap<Double, Hop> hopMap = new TreeMap<>();
/*
        for (WayPoint wp : itinerary.getWayPointList())
        {
            if (itinerary.getWayPointList().indexOf(wp)!=0)
            {
                double curDistance = DistanceBetweenMap(
                        itinerary.getWayPointList().get(itinerary.getWayPointList().indexOf(wp) - 1),
                        wp, matrix);
                double curTime = TimeBetweenMap(
                        itinerary.getWayPointList().get(itinerary.getWayPointList().indexOf(wp) - 1),
                        wp, matrix);
                Hop link = new Hop(
                        itinerary.getWayPointList().get(itinerary.getWayPointList().indexOf(wp) - 1),wp);
                hopMap.put(curDistance, link);
                totalDistance += curDistance;
                System.out.println(curDistance);
                totalTime += curTime;
                }
            System.out.printf("====== %s --- %f ---%f ======\n", itinerary.getName(), totalDistance, totalTime);
        }

 */
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
