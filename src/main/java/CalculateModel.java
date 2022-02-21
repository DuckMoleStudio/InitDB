import static loader.Calculation.*;

public class CalculateModel {
    public static void main(String[] args) {

        // CONTROLS
        final double speedRatio = 1.5;
        final String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        final String dir = "local/graphhopper";
        final int MetroCriteria = 150; // meters, if stop is within, then it's a metro stop
        final int StopDelay = 30; // sec, time loss for stopping
        final int PedestrianSpeed = 1; // in m/s, 1 equals 3.6 km/h but we have air distances so ok
        final int IntervalDummy = 600; // in case not available
        final int Radius = 500; // meters, looking for stops in this radius
        final int RadiusMetro = 6000; // meters, looking for metro in this radius
        final int SnapDistance = 500; // no road radius

        // route hops
        CalcTripHops(speedRatio, osmFile, dir);
        // stop dist to metro
        CalcStopMinDistToMetro();
        // trips from stops to metro
        CalcStopToMetro(MetroCriteria, StopDelay, PedestrianSpeed, IntervalDummy);
        // cell dist to active stops
        CalcCellMinDistToStop();
        // cell to metro
        CalcCellMetroAll(Radius, RadiusMetro, PedestrianSpeed, osmFile, dir, speedRatio, SnapDistance);
    }
}
