import static loader.ImportGTFS.*;

public class LoadGTFS {
    public static void main(String[] args) {

        // CONTROLS
        final String stopFile = "c:/matrix/GTFS/GTFS_STOPS.TXT";
        final String tripFile = "c:/matrix/GTFS/GTFS_TRIPS.TXT";
        final String nameFile = "c:/matrix/GTFS/GTFS_ROUTES.TXT";
        final String tripStopFile = "c:/matrix/GTFS/GTFS_TRIPS_STOPS.TXT";
        final String geomFile = "c:/matrix/GTFS/GTFS_TRIP_SHAPES.TXT";
        final String intervalFile = "c:/matrix/GTFS/GTFS_INTERVAL.TXT";

        // load stops
        ImportBusStops(stopFile);
        // load trips
        ImportTrips(tripFile);
        // load names
        ImportRouteNames(nameFile);
        // load trip stops
        ImportTripStops(tripStopFile);
        // delete tram
        DeleteTram();
        // load geom
        ImportTripGeom(geomFile);
        // load intervals
        ImportTripIntervals(intervalFile);
    }
}
