package loader;

import entity.BusStop;
import entity.Route;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static service.BusStopService.*;
import static service.RouteService.*;

public class ImportGTFS {

    public static void ImportBusStops(String filename) {

        int count=0;
        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                // Create entity
                final BusStop busStop = new BusStop();

                // Get significant fields
                busStop.setId(Integer.parseInt(values[0]));
                busStop.setName(values[2]);

                double lon = Double.parseDouble(values[5]);
                double lat = Double.parseDouble(values[4]);

                Coordinate pointCoord = new Coordinate(lon,lat);
                Point point = gf.createPoint(pointCoord);

                busStop.setGeom(point);

                addBusStop(busStop);
                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("\n\n===== Loaded %d bus stops in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }

    public static void ImportTrips(String filename) {

        int count=0;
        long startTime = System.currentTimeMillis();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                if(values[3].equals("00")) {
                    Route route = new Route();
                    route.setId(Integer.parseInt(values[0]));
                    route.setRid(values[1]);
                    route.setDir(values[4]);

                    addRoute(route);
                    count++;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.printf("\n\n===== Loaded %d trips in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }

    public static void ImportRouteNames(String filename) {

        int count=0;
        int countRoutes=0;
        long startTime = System.currentTimeMillis();
        List<Route> routes_all = listRoutes();

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                for(Route route: routes_all)
                {
                    if(values[0].equals(route.getRid()))
                    {
                        if(values[8].equals("Тм"))
                            route.setShortName("-Tm-");
                        else
                            route.setShortName(values[3]);
                        if(values[5].equals("межсубъектный"))
                            route.setShortName("-del-");

                        route.setLongName(values[4]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Route route: routes_all)
        {
            if(route.getShortName().equals("-del-"))
            {
                deleteRoute(route);
                countRoutes++;
            }
            else
            {
                updateRoute(route);
                count++;
            }
        }
        System.out.printf("\n\n===== Loaded %d route names and deleted %d unused routes in %d seconds ======\n\n"
                , count, countRoutes, (System.currentTimeMillis()-startTime)/1000);
    }

    public static void ImportTripStops(String filename) {

        int count=0;
        long startTime = System.currentTimeMillis();

        List<Route> routes = listRoutes();
        Map<String,List<String>> stopMap = new HashMap<>();
        for(Route route: routes)
            stopMap.put(String.valueOf(route.getId()),new ArrayList<>());

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                // if on valid set
                if (stopMap.containsKey(values[4]))
                    stopMap.get(values[4]).add(values[10]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Route route: routes)
        {
            route.setStops(stopMap.get(String.valueOf(route.getId())).toArray(new String[0]));
            updateRoute(route);
            count++;
        }

        System.out.printf("\n\n===== Loaded stops data for %d routes in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }

    public static void DeleteTram() {

        long startTime = System.currentTimeMillis();
        int countStops = 0;
        int countRoutes = 0;

        List<BusStop> stops = listBusStops();
        List<Route> routes = listRoutes();
        Map<String, BusStop> stopMap = new HashMap<>();

        for(BusStop stop: stops)
        {
            stop.setActive(false);
            stopMap.put(String.valueOf(stop.getId()), stop);
        }

        for(Route route: routes)
            if(!route.getShortName().equals("-Tm-"))
                for(String curStop: route.getStops())
                    if(stopMap.get(curStop)!=null)
                        stopMap.get(curStop).setActive(true);

        for(Route route: routes)
            if(route.getShortName().equals("-Tm-"))
            {
                for (String curStop : route.getStops())
                    if(stopMap.get(curStop)!=null&&!stopMap.get(curStop).isActive())
                        stopMap.get(curStop).setName("-del-");
                deleteRoute(route);
                countRoutes++;
            }

        for(BusStop stop: stops)
            if(stop.getName().equals("-del-"))
            {
                deleteBusStop(stop);
                countStops++;
            }
        System.out.printf("\n\n===== Removed %d tram stops and %d tram routes in %d seconds ======\n\n"
                , countStops, countRoutes, (System.currentTimeMillis()-startTime)/1000);
    }

    public static void ImportTripGeom(String filename) {

        int count=0;
        long startTime = System.currentTimeMillis();
        String prev_id = "definitely not an id";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine();
            String line;
            Route route = new Route();

            List<LineString> lines = new ArrayList<>();
            WKTReader geoReader = new WKTReader();
            GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                if(!values[1].equals(prev_id)) // new line with new track
                {
                    if(!firstLine && (route != null))
                    {
                        // save prev entry
                        route.setGeomML(gf.createMultiLineString(lines.toArray(new LineString[0])));

                        updateRoute(route);
                        count++;
                    }
                    // init new entry
                    prev_id=values[1];
                    firstLine=false;
                    route = getRouteById(Integer.parseInt(values[1]));

                    lines = new ArrayList<>();
                }

                if (route != null)
                {
                    lines.add((LineString) geoReader.read(values[4]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        System.out.printf("\n\n===== Loaded track data for %d routes in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }

    public static void ImportTripIntervals(String filename) {

        int count=0;
        long startTime = System.currentTimeMillis();

        List<Route> routes = listRoutes();
        Map<String,List<Integer>> intervalMap = new HashMap<>();
        for(Route route: routes)
            intervalMap.put(route.getRid(),new ArrayList<>());

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                List<Integer> curIntervals = intervalMap.get(values[7]);
                if(curIntervals!=null) {
                    curIntervals.add(Integer.parseInt(values[4]) * 60);
                    curIntervals.add(Integer.parseInt(values[5]) * 60);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Route route: routes)
        {
            List<Integer> curIntervals = intervalMap.get(route.getRid());
            route.setInterval(curIntervals.stream().mapToInt(Integer::intValue).average().orElse(Double.NaN));
            updateRoute(route);
            count++;
        }
        System.out.printf("\n\n===== Loaded intervals data for %d routes in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }

}

