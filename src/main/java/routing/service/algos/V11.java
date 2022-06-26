package routing.service.algos;

import entity.FishnetCellVer;
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

public class V11 extends Algo
{
    @Override
    public Result Calculate(List<WayPoint> wayPointList,
                            Map<WayPoint, MatrixLineMap> matrixIn,
                            AlgoParams paramsIn,
                            CellStopPattern cellStopPatternIn)
    {
        matrix = matrixIn;
        wayPoints = wayPointList;
        for(WayPoint wp: wayPoints)
            visitCount.put(wp,0);
        params = paramsIn;
        cellStopPattern = cellStopPatternIn;

        result = new Result();
        result.setMethodUsed("Simple Algorithm, linking all stops between terminals V11");

        InitTerminals();

        // MAKE A ROUTE FOR EACH STOP
        for(WayPoint wp: wayPoints)
            if(!terminalsAll.contains(wp))
            {
                boolean valid = false; // start & end are far enough from each other
                List<WayPoint> curTerminals = new ArrayList<>(terminalsAll);

                MatrixElement startME = NearestMapFrom(wp, matrix, curTerminals);
                MatrixElement endME = NearestMap(wp, matrix, curTerminals);
                WayPoint start = startME.getWayPoint();
                WayPoint end = endME.getWayPoint();

                if(start==null||end==null) break; // for matrix with no U-turns

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

                Itinerary it = buildItinerary(new ArrayList<>(List.of(start,wp,end)));

                it.setDir(100); // 100 for UNDEFINED

                result.getItineraries().add(it);
                result.setDistanceTotal(result.getDistanceTotal() + it.getDistance());
                result.setTimeTotal(result.getTimeTotal() + it.getTime());
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

        if(params.isPop())
            DiscardByPop();
        else
            DiscardByStops();

        if(params.isLog())
            printKPI(eval(result, matrix, cellStopPattern), "AFTER DISCARDING (2 PASS):");

        // FIND PAIRS OR CREATE REVERSE
        int routeCount = 1;
        for(Itinerary dirForward: new ArrayList<>(result.getItineraries()))
        {
            // search for pairs
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

            // assign reverse
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


                    Itinerary newReverse1 = buildItinerary(new ArrayList<>(List.of(endForward, startForward)));
                    newReverse1.setDir(1);
                    newReverse1.setRoute(routeCount++);

                List<WayPoint> reverseList = new ArrayList<>();
                reverseList.add(endForward);
                for (WayPoint forwardWP : dirForward.getWayPointList()) {
                    WayPoint reverseWP = null;
                    int occCount = 0;
                    for (WayPoint tryStop : stopsOnly)
                        if (tryStop.getDescription().equals(forwardWP.getDescription())
                                && !tryStop.equals(forwardWP) && stopsOnly.contains(forwardWP)) {
                            occCount++;
                            reverseWP = tryStop;
                        }
                    if (occCount == 1)
                        reverseList.add(1,reverseWP);
                }
                reverseList.add(startForward);
                Itinerary newReverse2 = buildItinerary(reverseList);
                newReverse2.setDir(8);

                if((newReverse2.getTime()/newReverse1.getTime())<params.getReverseDetour())
                {
                    newReverse2.setRoute(routeCount++);
                    result.getItineraries().add(newReverse2);
                    result.setDistanceTotal(result.getDistanceTotal() + newReverse2.getDistance());
                    result.setTimeTotal(result.getTimeTotal() + newReverse2.getTime());
                    result.setItineraryQty(result.getItineraryQty() + 1);
                }
                else
                {
                    newReverse1.setRoute(routeCount++);
                    result.getItineraries().add(newReverse1);
                    result.setDistanceTotal(result.getDistanceTotal() + newReverse1.getDistance());
                    result.setTimeTotal(result.getTimeTotal() + newReverse1.getTime());
                    result.setItineraryQty(result.getItineraryQty() + 1);
                }
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
