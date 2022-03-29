package routing.fileManagement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import routing.entity.WayPoint;
import routing.entity.storage.MatrixLineMap;

import java.io.*;
import java.util.List;
import java.util.Map;

public class SaveMatrixB {
    public static void saveB(
            List<WayPoint> wayPointList,
            Map<WayPoint, MatrixLineMap> matrix,
            String fileNameJSON,
            String fileNameBIN)
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // write waypoints
        try (FileWriter writer = new FileWriter(fileNameJSON)) {
            for (WayPoint wp : wayPointList)
            {
                writer.write(objectMapper.writeValueAsString(wp));
                writer.write("\n");
            }
            writer.flush();
            System.out.println("Saved waypoints as: " + fileNameJSON);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }

        // and binaries
        try (FileOutputStream fos = new FileOutputStream(fileNameBIN);
             DataOutputStream dos = new DataOutputStream(fos))
        {

            for (WayPoint wp : wayPointList)
            {
                for(WayPoint wwp : wayPointList)
                {
                    dos.writeDouble(matrix.get(wp).getDistances().get(wwp).getDistance());
                    dos.writeDouble(matrix.get(wp).getDistances().get(wwp).getTime());
                    //dd.add(matrix.get(wp).getDistances().get(wwp));
                }

            }

            System.out.println("Saved matrix as: " + fileNameBIN);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}