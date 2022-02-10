import entity.Route;
import entity.Route2;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static service.Route2Service.getRoute2ById;
import static service.Route2Service.updateRoute2;
import static service.RouteService.getRouteById;
import static service.RouteService.updateRoute;

public class ImportTripGeom {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();
        String prev_id = "definitely not an id";

        try (BufferedReader br = new BufferedReader(new FileReader("c:/matrix/GTFS/GTFS_TRIP_SHAPES.TXT"))) {
            br.readLine();
            String line;
            Route2 route = new Route2();

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

                        updateRoute2(route);
                        count++;

                    }
                    // init new entry
                    prev_id=values[1];
                    firstLine=false;
                    route = getRoute2ById(Integer.parseInt(values[1]));

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
}
