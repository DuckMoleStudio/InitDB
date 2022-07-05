package routing.service.algos;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.algorithm.state.StateManager;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.constraint.*;
import com.graphhopper.jsprit.core.problem.job.Delivery;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.job.Pickup;
import com.graphhopper.jsprit.core.problem.job.Shipment;
import com.graphhopper.jsprit.core.problem.misc.JobInsertionContext;
import com.graphhopper.jsprit.core.problem.solution.SolutionCostCalculator;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TimeWindow;
import com.graphhopper.jsprit.core.problem.solution.route.activity.TourActivity;
import com.graphhopper.jsprit.core.problem.vehicle.VehicleImpl;
import com.graphhopper.jsprit.core.reporting.SolutionPrinter;
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

public class V21 extends Algo
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

        List<WayPoint> stopsTo = new ArrayList<>();
        List<WayPoint> stopsFrom = new ArrayList<>();

        for(WayPoint stop: stopsOnly)
        {
            if(isTo(stop,terminalsAll))
                stopsTo.add(stop);
            else
                stopsFrom.add(stop);
        }

        System.out.printf("\nFrom %d stops total selected %d to & %d from\n",
                stopsOnly.size(),stopsTo.size(),stopsFrom.size());

        Map<WayPoint,Integer> pointValueMap = new HashMap<>();

        // todo: this should be stream

        for(WayPoint stop: new ArrayList<>(stopsTo))
        {
            int popCount=0;
            for(FishnetCellVer cell: cellStopPattern.getCellsForStop().get(stop))
                popCount+=cell.getHome();
            if(popCount<6000) stopsTo.remove(stop);
            else
                pointValueMap.put(stop,popCount);
        }

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
        List<WayPoint> realPickups = new ArrayList<>();
        for(WayPoint stop: newPickups)
        {
            if(isTo(stop,terminalsAll))
                realPickups.add(stop);
        }

        for(WayPoint stop: new ArrayList<>(stopsFrom))
        {
            int popCount=0;
            for(FishnetCellVer cell: cellStopPattern.getCellsForStop().get(stop))
                popCount+=cell.getHome();
            if(popCount<8000) stopsFrom.remove(stop);
        }
        System.out.printf("%d to & %d from left\n",realPickups.size(),stopsFrom.size());

        List<Pickup> pickups = new ArrayList<>();
        //List<Delivery> deliveries = new ArrayList<>();

        //int[] pointValues = new int[realPickups.size()+2];

        for(WayPoint wp: newPickups)
        {
            Pickup pickup = Pickup.Builder
                    .newInstance(String.valueOf(wayPoints.indexOf(wp)))
                    .addSizeDimension(0,1)
                    .setLocation(Location.newInstance(String.valueOf(wayPoints.indexOf(wp))))
                    .build();
            pickups.add(pickup);
            //pointValues[pickup.getIndex()]=pointValueMap.get(wp);

        }

/*
        for(WayPoint wp: stopsFrom)
        {
            deliveries.add(Delivery.Builder
                    .newInstance(String.valueOf(wayPoints.indexOf(wp)))
                    .addSizeDimension(0,1)
                    .setLocation(Location.newInstance(String.valueOf(wayPoints.indexOf(wp))))
                    .build());
        }

 */

        List<VehicleImpl> vehicles = new ArrayList<>();

        int cc = 0;

        for (WayPoint wp : terminalsAll)
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
        SoftActivityConstraint softStraight = new SoftActivityConstraint() {
            @Override
            public double getCosts(JobInsertionContext iFacts,
                                   TourActivity prevAct,
                                   TourActivity newAct,
                                   TourActivity nextAct,
                                   double prevActDepTime) {
                WayPoint endWP = wayPoints.get(Integer.parseInt(iFacts.getNewVehicle().getStartLocation().getId()));
                WayPoint prevWP = wayPoints.get(Integer.parseInt(prevAct.getLocation().getId()));
                WayPoint newWP = wayPoints.get(Integer.parseInt(newAct.getLocation().getId()));
                WayPoint nextWP = wayPoints.get(Integer.parseInt(nextAct.getLocation().getId()));

                if(prevWP.equals(endWP)&&nextWP.equals(endWP))
                    return 0;

                double prevDist = TimeBetweenMap(prevWP,nextWP,matrix);
                double newDist = TimeBetweenMap(newWP,nextWP,matrix);
                double newDistTo = TimeBetweenMap(prevWP,newWP,matrix);
                //double newDistToStart = TimeBetweenMap(newWP,endWP,matrix);
                //double nextDistToStart = TimeBetweenMap(nextWP,endWP,matrix);

                //double newDistFrom = TimeBetweenMap(newWP,prevWP,matrix);
                //double nextDist = TimeBetweenMap(nextWP,endWP,matrix);

                if(!prevWP.equals(endWP)) // not after start
                {
                    if((newDistTo+newDist-prevDist)<1000*params.getMaxDetour())
                    {
                        goodInserts++;
                        return -10000;
                    }
                    else
                    {
                        badInserts++;
                        return 10000;
                    }
                }
                else // between start and 1st
                {
                    badInserts++;
                    return 1000000;
                }
            }
        };

        SoftActivityConstraint farTerminal = new SoftActivityConstraint() {
            @Override
            public double getCosts(JobInsertionContext iFacts,
                                   TourActivity prevAct,
                                   TourActivity newAct,
                                   TourActivity nextAct,
                                   double prevActDepTime) {
                WayPoint endWP = wayPoints.get(Integer.parseInt(iFacts.getNewVehicle().getStartLocation().getId()));
                WayPoint prevWP = wayPoints.get(Integer.parseInt(prevAct.getLocation().getId()));
                WayPoint newWP = wayPoints.get(Integer.parseInt(newAct.getLocation().getId()));
                WayPoint nextWP = wayPoints.get(Integer.parseInt(nextAct.getLocation().getId()));

                if(prevWP.equals(endWP)&&nextWP.equals(endWP)) {
                    double tt = TimeBetweenMap(newWP, endWP, matrix);
                    if (tt < 5*60000)
                        return tt * (-1);
                }
                return 0;
            }
        };

        SoftRouteConstraint preferStraightRoutes = new SoftRouteConstraint() {
            @Override
            public double getCosts(JobInsertionContext iFacts) {
                VehicleRoute route = iFacts.getRoute();
                List<TourActivity> activities =  route.getActivities();

                if(activities.size()>0) {
                    double actTime = 0;
                    double shortTime = problem.getTransportCosts().getTransportCost(route.getStart().getLocation(),
                            activities.get(0).getLocation(), 0, route.getDriver(),
                            route.getVehicle());

                    TourActivity prevAct = route.getEnd();

                    for (int i = activities.size() - 1; i >= 0; i--) {
                        actTime += problem.getTransportCosts().getTransportCost(activities.get(i).getLocation(),
                                prevAct.getLocation(), prevAct.getEndTime(), route.getDriver(),
                                route.getVehicle()); // cur to prev
                        prevAct = activities.get(i);
                    }

                    return (actTime / shortTime) * 600000;
                }
                return 0;
            }
        };

        SoftRouteConstraint complyToKPI2 = new SoftRouteConstraint() {
            @Override
            public double getCosts(JobInsertionContext iFacts) {
                VehicleRoute route = iFacts.getRoute();
                List<TourActivity> activities =  route.getActivities();

                if(activities.size()>0) {
                    double curTime = 0;
                    double cost = 0;
                    TourActivity prevAct = route.getEnd();
                    for (int i = activities.size()-1; i >=0 ; i--) {
                        curTime+=problem.getTransportCosts().getTransportCost(activities.get(i).getLocation(),
                                prevAct.getLocation(), prevAct.getEndTime(), route.getDriver(),
                                route.getVehicle()); // cur to prev

                        if(curTime>5*60000)
                            cost+=1000000;

                        prevAct = activities.get(i);
                    }
                    return cost;
                }
                return 0;
            }
        };

        SoftRouteConstraint manyStopsAreGood = new SoftRouteConstraint() {
            @Override
            public double getCosts(JobInsertionContext iFacts) {
                VehicleRoute route = iFacts.getRoute();
                List<TourActivity> activities = route.getActivities();

                return activities.size() * 1000000 * (-1);
            }
        };

        SoftActivityConstraint straightestRouteSoft = new SoftActivityConstraint() {

            @Override
            public double getCosts(JobInsertionContext iFacts,
                                               TourActivity prevAct,
                                               TourActivity newAct,
                                               TourActivity nextAct,
                                               double prevActDepTime) {
                WayPoint endWP = wayPoints.get(Integer.parseInt(iFacts.getNewVehicle().getStartLocation().getId()));
                WayPoint prevWP = wayPoints.get(Integer.parseInt(prevAct.getLocation().getId()));
                WayPoint newWP = wayPoints.get(Integer.parseInt(newAct.getLocation().getId()));
                WayPoint nextWP = wayPoints.get(Integer.parseInt(nextAct.getLocation().getId()));

                if(prevWP.equals(endWP)&&nextWP.equals(endWP))
                    return 0;

                double prevDist = TimeBetweenMap(prevWP,nextWP,matrix);
                double newDist = TimeBetweenMap(newWP,nextWP,matrix);
                double newDistTo = TimeBetweenMap(prevWP,newWP,matrix);
                //double newDistToStart = TimeBetweenMap(newWP,endWP,matrix);
                //double nextDistToStart = TimeBetweenMap(nextWP,endWP,matrix);

                //double newDistFrom = TimeBetweenMap(newWP,prevWP,matrix);
                //double nextDist = TimeBetweenMap(nextWP,endWP,matrix);

                if(!prevWP.equals(endWP)) // not after start
                {
                    if((newDistTo+newDist-prevDist)<1000*params.getMaxDetour()&&newDist<prevDist)
                    {
                        goodInserts++;
                        return -1000000;
                    }
                    else
                    {
                        badInserts++;
                        return 0;
                    }
                }
                else // between start and 1st
                {
                    badInserts++;
                    return 10000000;
                }

            }
        };

        SoftActivityConstraint straighterRouteSoft = new SoftActivityConstraint() {

            @Override
            public double getCosts(JobInsertionContext iFacts,
                                   TourActivity prevAct,
                                   TourActivity newAct,
                                   TourActivity nextAct,
                                   double prevActDepTime) {
                WayPoint endWP = wayPoints.get(Integer.parseInt(iFacts.getNewVehicle().getStartLocation().getId()));
                WayPoint prevWP = wayPoints.get(Integer.parseInt(prevAct.getLocation().getId()));
                WayPoint newWP = wayPoints.get(Integer.parseInt(newAct.getLocation().getId()));
                WayPoint nextWP = wayPoints.get(Integer.parseInt(nextAct.getLocation().getId()));

                if(prevWP.equals(endWP)&&nextWP.equals(endWP))
                    return 0;

                double prevDist = TimeBetweenMap(prevWP,nextWP,matrix);
                double newDist = TimeBetweenMap(newWP,nextWP,matrix);
                double newDistTo = TimeBetweenMap(prevWP,newWP,matrix);
                //double newDistToStart = TimeBetweenMap(newWP,endWP,matrix);
                //double nextDistToStart = TimeBetweenMap(nextWP,endWP,matrix);

                //double newDistFrom = TimeBetweenMap(newWP,prevWP,matrix);
                //double nextDist = TimeBetweenMap(nextWP,endWP,matrix);

                if(!prevWP.equals(endWP)) // not after start
                {
                    if(newDistTo<prevDist&&newDist<prevDist)
                    {
                        goodInserts++;
                        return -1000000;
                    }
                    else
                    {
                        badInserts++;
                        return 0;
                    }
                }
                else // between start and 1st
                {
                    badInserts++;
                    return 10000000;
                }

            }
        };


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
                //double newDistToStart = TimeBetweenMap(newWP,endWP,matrix);
                //double nextDistToStart = TimeBetweenMap(nextWP,endWP,matrix);

                //double newDistFrom = TimeBetweenMap(newWP,prevWP,matrix);
                //double nextDist = TimeBetweenMap(nextWP,endWP,matrix);

                if(!prevWP.equals(endWP)) // not after start
                {
                    if((newDistTo+newDist-prevDist)<1000*params.getMaxDetour()&&newDist<prevDist)
                    {
                        goodInserts++;
                        return ConstraintsStatus.FULFILLED;
                    }
                    else
                    {
                        badInserts++;
                        return ConstraintsStatus.NOT_FULFILLED;
                    }
                }
                else // between start and 1st
                {
                    badInserts++;
                    return ConstraintsStatus.NOT_FULFILLED;
                }
/*
                {
                    if((newDistTo+newDist-prevDist)<1000*params.getMaxDetour())
                    {
                        goodInserts++;
                        return ConstraintsStatus.FULFILLED;
                    }
                    else
                    {
                        if((newDistToStart>newDist)&&(newDistToStart>nextDistToStart))
                        {
                            goodInserts++;
                            return ConstraintsStatus.FULFILLED;
                        }
                        badInserts++;
                        return ConstraintsStatus.NOT_FULFILLED;
                    }
                }

 */
            }
        };

        HardActivityConstraint straighterRoute = new HardActivityConstraint() {

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
                //double newDistToStart = TimeBetweenMap(newWP,endWP,matrix);
                //double nextDistToStart = TimeBetweenMap(nextWP,endWP,matrix);

                //double newDistFrom = TimeBetweenMap(newWP,prevWP,matrix);
                //double nextDist = TimeBetweenMap(nextWP,endWP,matrix);

                if(!prevWP.equals(endWP)) // not after start
                {
                    if((newDistTo+newDist)/prevDist<(1+prevDist/200000))
                    {
                        goodInserts++;
                        return ConstraintsStatus.FULFILLED;
                    }
                    else
                    {
                        badInserts++;
                        return ConstraintsStatus.NOT_FULFILLED;
                    }
                }
                else // between start and 1st
                {
                    badInserts++;
                    return ConstraintsStatus.NOT_FULFILLED;
                }
            }
        };

        constraintManager.addConstraint(straightestRoute, ConstraintManager.Priority.HIGH);
        //constraintManager.addConstraint(farTerminal);
        //constraintManager.addConstraint(straighterRouteSoft);
        //constraintManager.addConstraint(preferStraightRoutes);
        //constraintManager.addConstraint(complyToKPI2);
        //constraintManager.addConstraint(manyStopsAreGood);

        // OBJECTIVE FUNCTION
        SolutionCostCalculator customSolutionCostCalculator1 = new SolutionCostCalculator() {
            @Override
            public double getCosts(VehicleRoutingProblemSolution solution) {
                double costs = 0.;

                for (VehicleRoute route : solution.getRoutes()) {
                    //costs += route.getVehicle().getType().getVehicleCostParams().fix;
                    List<TourActivity> activities =  route.getActivities();
                    double curTime = 0;
                    TourActivity prevAct = route.getEnd();
                    for (int i = activities.size()-1; i >=0 ; i--) {
                       curTime+=problem.getTransportCosts().getTransportCost(activities.get(i).getLocation(),
                               prevAct.getLocation(), prevAct.getEndTime(), route.getDriver(),
                               route.getVehicle()); // cur to prev

                        costs+=curTime; //*pointValues[activities.get(i).getIndex()];
                        //if(curTime>5*60000) costs+=curTime*2;

                       prevAct = activities.get(i);
                    }

                    costs+=problem.getTransportCosts().getTransportCost(route.getStart().getLocation(),
                            activities.get(0).getLocation(), prevAct.getEndTime(), route.getDriver(),
                            route.getVehicle());
                    //*pointValues[activities.get(0).getIndex()]; // start to 1
                }
                for(Job j : solution.getUnassignedJobs()){
                    costs += 10*60000;//*pointValues[j.getIndex()];
                }
                //costs+=solution.getRoutes().size()*4*60000;
                return costs;
            }
        };

        SolutionCostCalculator customSolutionCostCalculator2 = new SolutionCostCalculator() {
            @Override
            public double getCosts(VehicleRoutingProblemSolution solution) {
                double costs = 0.;

                for (VehicleRoute route : solution.getRoutes()) {
                    //costs += route.getVehicle().getType().getVehicleCostParams().fix;
                    List<TourActivity> activities =  route.getActivities();
                    double curTime = 0;
                    TourActivity prevAct = route.getEnd();
                    for (int i = activities.size()-1; i >=0 ; i--) {
                        curTime+=problem.getTransportCosts().getTransportCost(activities.get(i).getLocation(),
                                prevAct.getLocation(), prevAct.getEndTime(), route.getDriver(),
                                route.getVehicle()); // cur to prev
                        if(curTime>8*60000)
                        costs+=curTime; //*pointValues[activities.get(i).getIndex()];
                        prevAct = activities.get(i);
                    }

                    costs+=problem.getTransportCosts().getTransportCost(route.getStart().getLocation(),
                            activities.get(0).getLocation(), prevAct.getEndTime(), route.getDriver(),
                            route.getVehicle())*0.1;
                    //*pointValues[activities.get(0).getIndex()]; // start to 1
                }
                for(Job j : solution.getUnassignedJobs()){
                    costs += 0.02*60000*params.getMaxDistance();//*pointValues[j.getIndex()];
                }
                return costs;
            }
        };

        //builder.setObjectiveFunction(costCalculator);

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


        List<WayPoint> terminalsMetro = new ArrayList<>();
        for (WayPoint wp : wayPoints)
            if (wp.getType() == WayPointType.METRO_TERMINAL)
                terminalsMetro.add(wp);

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

        System.out.printf("\nGood %d, Bad %d\n",goodInserts,badInserts);


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


        // FIND PAIRS OR CREATE REVERSE

        /*
        int routeCount = 1;
        for(Itinerary dirForward: new ArrayList<>(result.getItineraries()))
        {
            WayPoint startForward = dirForward.getFirst();
            WayPoint endForward = dirForward.getLast();
            Itinerary pair = null;
            for(Itinerary dirReverse: result.getItineraries())
            {
                WayPoint startReverse = dirReverse.getFirst();
                WayPoint endReverse = dirReverse.getLast();
                if(startForward.equals(endReverse)&&endForward.equals(startReverse)
                        &&dirForward.getDir()==100&&dirReverse.getDir()==100)
                {
                    pair = dirReverse;
                    break;
                }
            }
            if(pair!=null)
            {
                // make pair
                dirForward.setDir(0);
                dirForward.setRoute(routeCount);
                pair.setDir(1);
                pair.setRoute(routeCount++);
            }
            else
            if(dirForward.getDir()==100)
            {
                // make loop
                dirForward.setDir(0);
                dirForward.setRoute(routeCount);

                Itinerary newReverse = buildItinerary(new ArrayList<>(List.of(endForward,startForward)));
                newReverse.setDir(1);
                newReverse.setRoute(routeCount++);
                result.getItineraries().add(newReverse);
                result.setDistanceTotal(result.getDistanceTotal() + newReverse.getDistance());
                result.setTimeTotal(result.getTimeTotal() + newReverse.getTime());
                result.setItineraryQty(result.getItineraryQty() + 1);
            }
        }
        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "AFTER LINKING:");


         */



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
