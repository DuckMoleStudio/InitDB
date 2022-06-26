package routing.service.algos;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.algorithm.box.Jsprit;
import com.graphhopper.jsprit.core.problem.Location;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Delivery;
import com.graphhopper.jsprit.core.problem.job.Pickup;
import com.graphhopper.jsprit.core.problem.job.Shipment;
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

        // todo: this should be stream
        for(WayPoint stop: new ArrayList<>(stopsTo))
        {
            int popCount=0;
            for(FishnetCellVer cell: cellStopPattern.getCellsForStop().get(stop))
                popCount+=cell.getHome();
            if(popCount<8000) stopsTo.remove(stop);
        }
        for(WayPoint stop: new ArrayList<>(stopsFrom))
        {
            int popCount=0;
            for(FishnetCellVer cell: cellStopPattern.getCellsForStop().get(stop))
                popCount+=cell.getHome();
            if(popCount<8000) stopsFrom.remove(stop);
        }
        System.out.printf("%d to & %d from left\n",stopsTo.size(),stopsFrom.size());

        List<Pickup> pickups = new ArrayList<>();
        //List<Delivery> deliveries = new ArrayList<>();


        for(WayPoint wp: stopsTo)
        {
            pickups.add(Pickup.Builder
                    .newInstance(String.valueOf(wayPoints.indexOf(wp)))
                    .addSizeDimension(0,1)
                    .setLocation(Location.newInstance(String.valueOf(wayPoints.indexOf(wp))))
                    .build());
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
            itinerariesJS.add(curItinerary);
        }

        // COMPLETE ITINERARIES


        List<WayPoint> terminalsMetro = new ArrayList<>();
        for (WayPoint wp : wayPoints)
            if (wp.getType() == WayPointType.METRO_TERMINAL)
                terminalsMetro.add(wp);

        for (Itinerary itineraryJS : itinerariesJS)
        {
            /*
            WayPoint start = itinerary.getFirst();
            WayPoint end = itinerary.getLast();
            itinerary.setName("***  " + start.getDescription() + " - " + end.getDescription());


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

            FillLink(itinerary, 50, false, 0);


             */
            Itinerary itinerary = buildItinerary(itineraryJS.getWayPointList());

            itinerary.setId(itCount++);
            //itinerary.setRoute(routeCount++);
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


        // FIND PAIRS OR CREATE REVERSE

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
