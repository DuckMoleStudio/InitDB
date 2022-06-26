package routing.service.algos;

import routing.entity.WayPoint;
import routing.entity.WayPointType;
import routing.entity.eval.AlgoParams;
import routing.entity.eval.CellStopPattern;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;
import routing.entity.storage.MatrixElement;
import routing.entity.storage.MatrixLineMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static routing.service.Evaluate.eval;
import static routing.service.Matrix.*;
import static routing.service.Matrix.DistanceBetweenMap;

public class V12 extends Algo
{
    @Override
    public Result Calculate(List<WayPoint> wayPointList,
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
        result.setMethodUsed("Simple Algorithm, linking all stops to all terminals in range V12");

        InitTerminals();

        // MAKE ALL VALID ROUTES FOR EACH STOP
        for(WayPoint stop: stopsOnly)
            for(WayPoint tryEnd: terminalsAll)
                for(WayPoint tryStart: terminalsAll)
                {
                    boolean startValid = TimeBetweenMap(tryStart,stop,matrix) < TimeBetweenMap(stop,tryStart,matrix);
                    boolean endValid = TimeBetweenMap(tryEnd,stop,matrix) > TimeBetweenMap(stop,tryEnd,matrix);
                    double tryTime1 = TimeBetweenMap(tryStart,stop,matrix);
                    double tryTime2 = TimeBetweenMap(stop,tryEnd,matrix);
                    double tryTimeTerm = TimeBetweenMap(tryStart, tryEnd,matrix);
                    boolean inRange = tryTimeTerm > params.getMinDistance()*60000 && tryTimeTerm < params.getMaxDistance()*60000
                            && tryTime1 < params.getMaxDistance()*60000 && tryTime2 < params.getMaxDistance()*60000;
                    boolean oneMetro = tryEnd.getType().equals(WayPointType.METRO_TERMINAL)
                            ||tryStart.getType().equals(WayPointType.METRO_TERMINAL);
                    if(startValid && endValid && inRange && oneMetro)
                    {
                        Itinerary it = buildItinerary(new ArrayList<>(List.of(tryStart,stop,tryEnd)));

                        it.setDir(100); // 100 for UNDEFINED

                        result.getItineraries().add(it);
                        result.setDistanceTotal(result.getDistanceTotal() + it.getDistance());
                        result.setTimeTotal(result.getTimeTotal() + it.getTime());
                        result.setItineraryQty(result.getItineraryQty() + 1);
                    }
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
