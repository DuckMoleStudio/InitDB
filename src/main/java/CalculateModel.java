import entity.Version;

import java.sql.Date;

import static loader.Calculation.*;
import static service.VersionService.*;

public class CalculateModel {
    public static void main(String[] args) {

        // CONTROLS
        final double speedRatio = 1.7;
        final String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        final String dir = "local/graphhopper";
        final int MetroCriteria = 150; // meters, if stop is within, then it's a metro stop
        final int StopDelay = 30; // sec, time loss for stopping
        final int PedestrianSpeed = 1; // in m/s, 1 equals 3.6 km/h but we have air distances so ok
        final int IntervalDummy = 600; // in case not available
        final int Radius = 500; // meters, looking for stops in this radius
        final int RadiusMetro = 2000; // meters, looking for metro in this radius
        final int SnapDistance = 500; // no road radius

        // route hops
        //CalcTripHops(speedRatio, StopDelay, osmFile, dir);
        //CalcTripHopsVer(speedRatio, StopDelay, osmFile, dir);

        // stop dist to metro
        //CalcStopMinDistToMetro();
        //CalcStopMinDistToMetroVer();



        // trips from stops to metro
        //CalcStopToMetro(MetroCriteria, StopDelay, PedestrianSpeed, IntervalDummy);
        //CalcStopToMetroVer(MetroCriteria, StopDelay, PedestrianSpeed, IntervalDummy, 5);

        // cell dist to active stops
        //CalcCellMinDistToStop();
        CalcCellMinDistToStopHS(4);

        /*
        Version version = new Version();
        version.setDesc("Radius for metro/road 600m, metro search radius for car 3000m");
        version.setDate(Date.valueOf("2022-2-24"));
         */

        //CalcCellMinDistToStopVer(version);



        //deleteVersion(version);

        // cell to metro
        //CalcCellMetroAllVer(Radius, RadiusMetro, PedestrianSpeed, osmFile, dir, speedRatio, SnapDistance, version);
        //CalcCellMetroAllHS(Radius, RadiusMetro, PedestrianSpeed, osmFile, dir, speedRatio, SnapDistance, 5);
    }
}
