
import entity.Route;
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


import static service.RouteService.getRouteById;
import static service.RouteService.updateRoute;

// DEPRECATED


public class ImportTripData {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();
        String prev_id = "definitely not an id";

        try (BufferedReader br = new BufferedReader(new FileReader("c:/matrix/GTFS_TRIP_SHAPES"))) {
            br.readLine();
            String line;
            Route route = new Route();
            List<String> stops = new ArrayList<>();
            List<Double> distances = new ArrayList<>();
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
                        route.setStops(stops.toArray(new String[0])); // https://shipilev.net/blog/2016/arrays-wisdom-ancients/#_conclusion
                        double[] dd = new double[distances.size()];
                        for(int i=0;i< distances.size();i++) dd[i]=distances.get(i);
                        route.setDistances(dd);
                        route.setGeomML(gf.createMultiLineString(lines.toArray(new LineString[0])));

                        updateRoute(route);
                        count++;

                    }
                    // init new entry
                    prev_id=values[1];
                    firstLine=false;
                    route = getRouteById(Integer.parseInt(values[1]));
                    stops = new ArrayList<>();
                    distances = new ArrayList<>();
                    lines = new ArrayList<>();
                }

                if (route != null)
                {
                    if(values[6] != "")
                    {
                        String[] ss = values[6].split(",");
                        for(String s: ss) stops.add(s);
                    }

                    if(values[7] != "")
                    {
                        String[] ss = values[7].split(",");
                        for(String s: ss) distances.add(Double.valueOf(s));
                    }

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
