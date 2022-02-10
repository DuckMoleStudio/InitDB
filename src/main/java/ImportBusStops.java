import entity.BusStop;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import static service.BusStopService.addBusStop;

public class ImportBusStops {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        try (BufferedReader br = new BufferedReader(new FileReader("c:/matrix/GTFS/GTFS_STOPS.TXT"))) {
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
}
