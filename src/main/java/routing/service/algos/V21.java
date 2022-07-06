package routing.service.algos;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.*;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Pickup;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.util.Solutions;
import entity.FishnetCellVer;
import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.eval.AlgoParams;
import routing.entity.eval.CellStopPattern;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.storage.MatrixLineMap;

import java.util.*;

import static routing.service.Evaluate.eval;
import static routing.service.Matrix.DistanceBetweenMap;
import static routing.service.Matrix.TimeBetweenMap;

public class V21 extends AlgoJS
{
    @Override
    public Result Calculate
            (List<WayPoint> wayPointList,
             Map<WayPoint, MatrixLineMap> matrixIn,
             AlgoParams paramsIn,
             CellStopPattern cellStopPattern)
    {
        matrix = matrixIn;
        wayPoints = wayPointList;
        for(WayPoint wp: wayPoints)
            visitCount.put(wp,0);
        params = paramsIn;

        result = new Result();
        result.setMethodUsed("Jsprit delivery w/time, * stop->metro V21");

        InitTerminals();
        InitJsprit();

        // skeletal stop set
        int threshold = 1000;
        List<WayPoint> newPickups = new ArrayList<>();
        for(FishnetCellVer cell: cellStopPattern.getCells())
        {
            List<WayPoint> stops = cellStopPattern.getStopsForCell().get(cell);
            if(stops.size()>0)
            {
                if(stops.size()==1)
                {
                    WayPoint stop = stops.get(0);
                    if(cell.getHome()>threshold&&!newPickups.contains(stop)&&stop.getType()==WayPointType.STOP)
                        newPickups.add(stops.get(0));
                }
                else
                {
                    boolean present = false;
                    int maxPop = 0;
                    WayPoint biggest = null;
                    for(WayPoint stop: stops) {
                        if (newPickups.contains(stop)) present = true;
                        else {
                            int pop = 0;
                            for (FishnetCellVer cc : cellStopPattern.getCellsForStop().get(stop))
                                pop += cc.getHome();
                            if(pop>maxPop&&stop.getType()==WayPointType.STOP)
                            {
                                maxPop=pop;
                                biggest=stop;
                            }
                        }
                    }
                   if(!present&&biggest!=null) newPickups.add(biggest);
                }
            }
        }

        List<Pickup> pickups = new ArrayList<>();

        for(WayPoint wp: newPickups)
        {
            Pickup pickup = Pickup.Builder
                    .newInstance(String.valueOf(wayPoints.indexOf(wp)))
                    .addSizeDimension(0,1)
                    .setLocation(Location.newInstance(String.valueOf(wayPoints.indexOf(wp))))
                    .build();
            pickups.add(pickup);
        }

        List<VehicleImpl> vehicles = new ArrayList<>();

        int cc = 0;

        List<WayPoint> allMetro = new ArrayList<>();
        for(WayPoint wp: wayPoints)

            if (wp.getType() == WayPointType.METRO_TERMINAL)
                allMetro.add(wp);


        for (WayPoint wp : terminalsAll) // terminalsAll || allMetro
        {
            String routeName = wp.getDescription() + " " + cc++;
            VehicleImpl.Builder vehicleBuilder = VehicleImpl.Builder.newInstance(routeName);
            String start = String.valueOf(wayPoints.indexOf(wp));
            vehicleBuilder.setStartLocation(Location.newInstance(start));
            vehicleBuilder.setType(vehicleTypeMoscowBus);
            vehicleBuilder.setLatestArrival(params.getMaxDistance()*60000);
            vehicles.add(vehicleBuilder.build());
        }

        // --- set up routing problem
        VehicleRoutingProblem.Builder vrpBuilder = VehicleRoutingProblem.Builder.newInstance()
                .setFleetSize(VehicleRoutingProblem.FleetSize.INFINITE)
                .setRoutingCost(costMatrix);

        // ----- add cars and services (points) to the problem

        for (VehicleImpl vehicle : vehicles)
            vrpBuilder.addVehicle(vehicle);
        for (Pickup ss : pickups) vrpBuilder.addJob(ss);
        //for (Delivery ss : deliveries) vrpBuilder.addJob(ss);


        VehicleRoutingProblem problem = vrpBuilder.build();

        StateManager stateManager = new StateManager(problem);
        ConstraintManager constraintManager = new ConstraintManager(problem,stateManager);

        // CONSTRAINTS

        HardActivityConstraint straightestRoute = new HardActivityConstraint() {

            @Override
            public ConstraintsStatus fulfilled(JobInsertionContext iFacts,
                                   TourActivity prevAct,
                                   TourActivity newAct,
                                   TourActivity nextAct,
                                   double prevActDepTime) {
                WayPoint endWP = wayPoints.get(Integer.parseInt(iFacts.getNewVehicle().getStartLocation().getId()));
                WayPoint prevWP = wayPoints.get(Integer.parseInt(prevAct.getLocation().getId()));
                WayPoint newWP = wayPoints.get(Integer.parseInt(newAct.getLocation().getId()));
                WayPoint nextWP = wayPoints.get(Integer.parseInt(nextAct.getLocation().getId()));

                if(prevWP.equals(endWP)&&nextWP.equals(endWP))
                    return ConstraintsStatus.FULFILLED;

                double prevDist = TimeBetweenMap(prevWP,nextWP,matrix);
                double newDist = TimeBetweenMap(newWP,nextWP,matrix);
                double newDistTo = TimeBetweenMap(prevWP,newWP,matrix);

                if(!prevWP.equals(endWP)) // not after start
                {
                    if((newDistTo+newDist-prevDist)<1000*params.getMaxDetour()&&newDist<prevDist)
                        return ConstraintsStatus.FULFILLED;
                    else
                        return ConstraintsStatus.NOT_FULFILLED;
                }
                else // between start and 1st
                    return ConstraintsStatus.NOT_FULFILLED;
            }
        };

        constraintManager.addConstraint(straightestRoute, ConstraintManager.Priority.HIGH);

        // OBJECTIVE FUNCTION
        SolutionCostCalculator customSolutionCostCalculator1 = new SolutionCostCalculator() {
            @Override
            public double getCosts(VehicleRoutingProblemSolution solution) {
                double costs = 0.;

                for (VehicleRoute route : solution.getRoutes()) {
                    List<TourActivity> activities =  route.getActivities();
                    double curTime = 0;
                    TourActivity prevAct = route.getEnd();
                    for (int i = activities.size()-1; i >=0 ; i--) {
                       curTime+=problem.getTransportCosts().getTransportCost(activities.get(i).getLocation(),
                               prevAct.getLocation(), prevAct.getEndTime(), route.getDriver(),
                               route.getVehicle()); // cur to prev

                        costs+=curTime;

                       prevAct = activities.get(i);
                    }

                    costs+=problem.getTransportCosts().getTransportCost(route.getStart().getLocation(),
                            activities.get(0).getLocation(), prevAct.getEndTime(), route.getDriver(),
                            route.getVehicle()); // start to 1
                }
                for(Job j : solution.getUnassignedJobs()){
                    costs += 10*60000;
                }

                return costs;
            }
        };

        VehicleRoutingAlgorithm algorithm = Jsprit.Builder.newInstance(problem)
                .setStateAndConstraintManager(stateManager,constraintManager)
                .setObjectiveFunction(customSolutionCostCalculator1)
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

        for (VehicleRoute route : routesJS)
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

            jobId = route.getEnd().getLocation().getId();
            curItinerary.getWayPointList().add(wayPointList.get(Integer.parseInt(jobId)));

            itinerariesJS.add(curItinerary);
        }

        // COMPLETE ITINERARIES

            int routeCount = 1;

        for (Itinerary itinerary : itinerariesJS)
        {

            WayPoint start = itinerary.getFirst();
            WayPoint end = itinerary.getLast();
            itinerary.setName(itinerary.getWayPointList().get(1).getDescription() + " - " + end.getDescription());


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


            if (start.equals(itinerary.getWayPointList().get(1)))
                itinerary.getWayPointList().remove(1);
            if (!end.equals(start)) {
                itinerary.getWayPointList().add(start);
                itinerary.setDistance(itinerary.getDistance() +
                        DistanceBetweenMap(end, start, matrix));
                itinerary.setTime(itinerary.getTime() +
                        TimeBetweenMap(end, start, matrix));
            }

            FillLink(itinerary,  false, 0);



            //Itinerary itinerary = buildItinerary(itineraryJS.getWayPointList());

            itinerary.setId(itCount++);
            itinerary.setRoute(routeCount++);
            itinerary.setDir(0);

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
            if(redCount<1)
                removeItinerary(ii);
        }

        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "AFTER DISCARDING (1 PASS):");

        for(Itinerary ii: new ArrayList<>(result.getItineraries()))
        {
            int redCount = 0;
            for (WayPoint wp : ii.getWayPointList())
                if (visitCount.get(wp) < 2) redCount++;
            if(redCount<params.getRemoveWithLessUnique())
                removeItinerary(ii);
        }

        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "AFTER DISCARDING (2 PASS):");


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
}
