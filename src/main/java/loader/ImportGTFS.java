package loader;

import entity.*;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static service.BusStopHSService.*;
import static service.BusStopService.*;
import static service.RouteNameService.getRouteNameById;
import static service.RouteNameService.updateRouteName;
import static service.RouteService.*;
import static service.TripService.*;

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

    public static void ImportBusStopsVer(String filename, int versionId) {

        int count=0;
        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                // Create or load entity
                BusStopHS busStop = new BusStopHS();

                // Get significant fields
                busStop.setId(Integer.parseInt(values[0]));
                busStop.setName(values[2]);

                double lon = Double.parseDouble(values[5]);
                double lat = Double.parseDouble(values[4]);

                Coordinate pointCoord = new Coordinate(lon,lat);
                Point point = gf.createPoint(pointCoord);

                busStop.setGeom(point);

                updateBusStopHS(busStop);
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

    public static void ImportRoutesTripsVer(
            String routeFile,
            String tripFile,
            Map<Integer,String> excludeRoutes,
            Map<Integer,String> includeTrips,
            int versionId
    ) {
        int countRoutes=0;
        int countTrips=0;
        long startTime = System.currentTimeMillis();

        Map<Integer,RouteName> routes = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(routeFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                // conditions
                boolean valid = true;
                for(Map.Entry<Integer,String> me: excludeRoutes.entrySet())
                    if(values[me.getKey()].equals(me.getValue()))  valid=false;

                //if(!values[8].equals("Тм")&&!values[5].equals("межсубъектный"))
                if(valid)
                {
                    // Create or load entity
                    RouteName route = getRouteNameById(Integer.parseInt(values[0]));
                    if (route == null) {
                        route = new RouteName();
                        route.setShortName(values[3]);
                        route.setLongName(values[4]);
                        route.setId(Integer.parseInt(values[0]));
                    }
                    route.getTrips().put(versionId,new ArrayList<>());
                    routes.put(route.getId(),route);
                    countRoutes++;
                }

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        try (BufferedReader br = new BufferedReader(new FileReader(tripFile))) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                boolean update = false;
                Trip trip = new Trip();
                trip.setId(Integer.parseInt(values[0]));
                trip.setRid(values[1]);
                trip.setDir(values[4]);

                if(getTripById(Integer.parseInt(values[0]))!=null)
                {
                    trip = getTripById(Integer.parseInt(values[0]));
                    update=true;
                }

                if(trip.getVersions() == null) trip.setVersions(new HashMap<>());
                trip.getVersions().put(versionId,false);

                //condition
                boolean valid = false;
                for(Map.Entry<Integer,String> me: includeTrips.entrySet())
                    if(values[me.getKey()].equals(me.getValue()))  valid=true;

                //if(values[3].equals("00"))
                if(valid && routes.containsKey(Integer.valueOf(values[1])))
                {
                        routes.get(Integer.valueOf(values[1])).getTrips().get(versionId).add(Integer.valueOf(values[0]));
                        trip.getVersions().put(versionId,true);
                        update=true;
                    countTrips++;
                }
                if(update)
                updateTrip(trip);

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        for(Map.Entry<Integer,RouteName> me: routes.entrySet())
        {
            updateRouteName(me.getValue());

        }


        System.out.printf("\n\n===== Loaded %d routes & %d trips in %d seconds ======\n\n"
                , countRoutes, countTrips, (System.currentTimeMillis()-startTime)/1000);
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

    public static void ImportTripStopsVer(String filename) {

        int count=0;
        long startTime = System.currentTimeMillis();

        List<Trip> trips = listTrips();
        Map<String,List<String>> stopMap = new HashMap<>();
        for(Trip trip: trips)
            stopMap.put(String.valueOf(trip.getId()),new ArrayList<>());

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine();
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                // if on valid set

                BusStopHS stopValidate = getBusStopHSById(Integer.parseInt(values[10]));
                if (stopMap.containsKey(values[4])&&stopValidate!=null)
                    stopMap.get(values[4]).add(values[10]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(Trip trip: trips)
        {
            trip.setStops(stopMap.get(String.valueOf(trip.getId())).toArray(new String[0]));
            updateTrip(trip);
            count++;
        }

        System.out.printf("\n\n===== Loaded stops data for %d trips in %d seconds ======\n\n"
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

    public static void ImportTripGeomVer(String filename) {

        int count=0;
        long startTime = System.currentTimeMillis();
        String prev_id = "definitely not an id";

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            br.readLine();
            String line;
            Trip trip = new Trip();

            List<LineString> lines = new ArrayList<>();
            WKTReader geoReader = new WKTReader();
            GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                String[] values = line.split(";");

                if(!values[1].equals(prev_id)) // new line with new track
                {
                    if(!firstLine && (trip != null))
                    {
                        // save prev entry
                        trip.setGeomML(gf.createMultiLineString(lines.toArray(new LineString[0])));

                        updateTrip(trip);
                        count++;
                    }
                    // init new entry
                    prev_id=values[1];
                    firstLine=false;
                    trip = getTripById(Integer.parseInt(values[1]));

                    lines = new ArrayList<>();
                }

                if (trip != null)
                {
                    lines.add((LineString) geoReader.read(values[4]));
                }
            }

            // save last
            trip.setGeomML(gf.createMultiLineString(lines.toArray(new LineString[0])));
            updateTrip(trip);
            count++;

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

    public static void ImportTripIntervalsVer(String filename) {

        int count=0;
        long startTime = System.currentTimeMillis();

        List<Trip> trips = listTrips();
        Map<String,List<Integer>> intervalMap = new HashMap<>();
        for(Trip trip: trips)
            intervalMap.put(trip.getRid(),new ArrayList<>());

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

        for(Trip trip: trips)
        {
            List<Integer> curIntervals = intervalMap.get(trip.getRid());
            trip.setInterval(curIntervals.stream().mapToInt(Integer::intValue).average().orElse(Double.NaN));
            updateTrip(trip);
            count++;
        }
        System.out.printf("\n\n===== Loaded intervals data for %d routes in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);
    }

}

