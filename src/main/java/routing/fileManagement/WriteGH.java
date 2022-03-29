package routing.fileManagement;

import routing.entity.WayPoint;
import routing.entity.result.Itinerary;
import routing.entity.result.Result;

import java.io.FileWriter;
import java.io.IOException;

public class WriteGH {
    public static void write(Result rr, String urlOutputFile)
    {
        try (FileWriter writer = new FileWriter(urlOutputFile))
        {
            for (Itinerary iii : rr.getItineraries())
            {
                String url = "https://graphhopper.com/maps/?";
                for(WayPoint wp: iii.getWayPointList())
                {
                    url+="point=";
                    url+=wp.getLat();
                    url+="%2C";
                    url+=wp.getLon();
                    url+="&";
                }
                url+="locale=ru-RU&profile=car&use_miles=false";

                writer.write(url);
                writer.write("\n\n");
            }
            writer.flush();
        }
        catch (IOException ex)
        {
            System.out.println(ex.getMessage());
        }
        System.out.println("\nSaved as: " + urlOutputFile + "\n");
    }
}