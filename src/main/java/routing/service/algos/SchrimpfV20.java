package routing.service.algos;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.cost.VehicleRoutingTransportCosts;
import com.graphhopper.jsprit.core.problem.job.Service;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleType;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleTypeImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
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
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;
import routing.service.Matrix;

import java.util.*;

import static java.lang.Math.round;
import static routing.service.Evaluate.eval;
import static routing.service.Matrix.DistanceBetweenMap;
import static routing.service.Matrix.TimeBetweenMap;

public class SchrimpfV20 {

    public static Map<WayPoint, MatrixLineMap> matrix;
    public static Result result, finalResult;
    public static Map<WayPoint,Integer> visitCount = new HashMap<>();
    public static int discardCount;

    public static Result Calculate(
            List<WayPoint> wayPointList,
            Map<WayPoint, MatrixLineMap> matrixIn,
            AlgoParams params,
            CellStopPattern cellStopPattern)
    {
        long elTime = System.currentTimeMillis();
        matrix = matrixIn;
        for(WayPoint wp: wayPointList)
            visitCount.put(wp,0);

        // ---- prepare services (points) and get bases for car start ----
        List<Service> services = new ArrayList<>();
        List<WayPoint> terminals = new ArrayList<>();
        for(WayPoint c_wp: wayPointList)
        {
            if (c_wp.getType() == WayPointType.TERMINAL)
            {
                terminals.add(c_wp);
            }
            if (c_wp.getType() == WayPointType.METRO_TERMINAL)
            {
                services.add(Service.Builder
                        .newInstance(String.valueOf(wayPointList.indexOf(c_wp)))
                        .addSizeDimension(0, 1)
                        .setLocation(Location.newInstance(String.valueOf(wayPointList.indexOf(c_wp))))
                        .build());
            }
        }

        System.out.printf("\nSelected %d start and %d end points\n",terminals.size(),services.size());

        /*
         * get a vehicle type-builder and build a type
         */
        VehicleTypeImpl.Builder vehicleTypeBuilder = VehicleTypeImpl.Builder.newInstance("Moscow Bus Type")
                .addCapacityDimension(0, params.getCapacity());
        VehicleType vehicleTypeMoscowBus = vehicleTypeBuilder.build();

        /*
         * get a vehicle-builder and build vehicles
         */

        List<VehicleImpl> vehicles = new ArrayList<>();

        int cc=0;

        for (WayPoint wp: terminals)
        {
            String routeName = wp.getDescription() + " " + cc++;
            VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(routeName);
            String start = String.valueOf(wayPointList.indexOf(wp));
            vehicleBuilder.setStartLocation(Location.newInstance(start));
            vehicleBuilder.setType(vehicleTypeMoscowBus);
            vehicles.add(vehicleBuilder.build());
        }


        //define a matrix-builder building a NON-symmetric matrix
        VehicleRoutingTransportCostsMatrix.Builder costMatrixBuilder = VehicleRoutingTransportCostsMatrix
                .Builder.newInstance(false);

        System.out.print("\nBuilding matrix...");

        // ---- transfer our matrix to jsprit matrix ----
        for(int jj=0;jj<wayPointList.size();jj++)
            for(int kk=0;kk<wayPointList.size();kk++)
                if(jj!=kk)
                {
                    costMatrixBuilder.addTransportDistance(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Matrix.DistanceBetweenMap(wayPointList.get(jj),wayPointList.get(kk),matrix));
                    costMatrixBuilder.addTransportTime(
                            String.valueOf(jj),
                            String.valueOf(kk),
                            Matrix.TimeBetweenMap(wayPointList.get(jj),wayPointList.get(kk),matrix));
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


        // --- SET UP THE ROUTING PROBLEM ----
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance()
                .setFleetSize(VehicleRoutingProblem.FleetSize.INFINITE)
                .setRoutingCost(costMatrix);

        // ----- add cars and services (points) to the problem

        for(VehicleImpl vehicle: vehicles)
            vrpBuilder.addVehicle(vehicle);
        for(Service ss: services)
            vrpBuilder.addJob(ss);


        VehicleRoutingProblem problem = vrpBuilder.build();

        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
                .setProperty(Jsprit.Parameter.THREADS, "4")
                .buildAlgorithm();
        algorithm.setMaxIterations(params.getIterations());

        /*
         * and search a solution
         */
        Collection<VehicleRoutingProblemSolution> solutions = algorithm.searchSolutions();

        /*
         * get the best
         */
        VehicleRoutingProblemSolution bestSolution = Solutions.bestOf(solutions);

        //SolutionPrinter.print(problem, bestSolution, SolutionPrinter.Print.VERBOSE);

        // --- PARSE SOLUTION TO ITINERARIES ----
        List<Itinerary> itineraries = new ArrayList<>();

        List<VehicleRoute> routes = new ArrayList<VehicleRoute>(bestSolution.getRoutes());
        Collections.sort(routes, new com.graphhopper.jsprit.core.util.VehicleIndexComparator());

        for (VehicleRoute route : routes)
        {
            Itinerary curItinerary = new Itinerary();

            TourActivity prevAct = route.getStart();
            String jobId;
                jobId = prevAct.getLocation().getId();
                curItinerary.getWayPointList().add(wayPointList.get(Integer.parseInt(jobId)));

            for (TourActivity act : route.getActivities())
            {
                if (act instanceof TourActivity.JobActivity)
                {
                    jobId = ((TourActivity.JobActivity) act).getJob().getId();
                    curItinerary.getWayPointList().add(wayPointList.get(Integer.parseInt(jobId)));
                }
                prevAct = act;
            }
            itineraries.add(curItinerary);
        }

        int itCount=1;
        result = new Result();

        for(Itinerary itinerary: itineraries)
        {
            WayPoint start = itinerary.getFirst();
            WayPoint end = itinerary.getLast();
            itinerary.setName(start.getDescription() + " - " + end.getDescription());

            itinerary.setDistance(DistanceBetweenMap(start, end, matrix));
            itinerary.setTime(TimeBetweenMap(start, end, matrix));

            visitCount.put(start, visitCount.get(start) + 1);
            visitCount.put(end, visitCount.get(end) + 1);
/*
            if(!nodes.containsKey(start))
                nodes.put(start,new TrackList());
            if(!nodes.containsKey(end))
                nodes.put(end,new TrackList());

            nodes.get(start).getForward().add(itinerary);
            nodes.get(end).getReverse().add(itinerary);
 */

            FillLink(itinerary, 50, false, 0);

            itinerary.setId(itCount++);

            result.getItineraries().add(itinerary);
            result.setDistanceTotal(result.getDistanceTotal() + itinerary.getDistance());
            result.setTimeTotal(result.getTimeTotal() + itinerary.getTime());
            result.setItineraryQty(result.getItineraryQty() + 1);
        }

        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "BEFORE:");

        // ---- complete result -----


        long elapsedTime = System.currentTimeMillis() - elTime;
        System.out.println("\n\nTotal time: " + round(result.getTimeTotal()) / 60000 + " min");
        System.out.println("Total distance: " + round(result.getDistanceTotal()) / 1000 + " km");
        System.out.println("Cars assigned: " + result.getItineraryQty());
        System.out.println("Calculated in: " + elapsedTime + " ms\n");

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

        //nodes.get(it.getFirst()).getForward().remove(it);
        //nodes.get(it.getLast()).getReverse().remove(it);

        //System.out.println("-- "+it.getId());
    }
}
