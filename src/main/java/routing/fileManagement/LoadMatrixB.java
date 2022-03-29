package routing.fileManagement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.graphhopper.util.StopWatch;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import routing.entity.WayPoint;
import routing.entity.storage.MatrixLineMap;
import routing.entity.storage.TimeDistancePair;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadMatrixB {
    public static void restoreB(List<WayPoint> wayPointList,
                                Map<WayPoint, MatrixLineMap> matrix,
                                String jsonInputFile,
                                String binInputFile
    ) throws Exception
    {
        StopWatch sw = new StopWatch().start();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        //List<MatrixStorageLine> inMatrix = new ArrayList<>();

        List<String> inStrings = new ArrayList<>();

        /*
        try {
            inStrings = Files.readAllLines(Paths.get(jsonInputFile));
        } catch (IOException e)
        {
           throw e;
        }
         */

        LineIterator it = null;
        try {
            it = FileUtils.lineIterator(new File(jsonInputFile));
        } catch (IOException e) {
            throw e;
        }
        try {
            while (it.hasNext()) {
                inStrings.add(it.nextLine());
            }
        } finally {
            LineIterator.closeQuietly(it);
        }


        for (String ss : inStrings) {
            try {
                wayPointList.add(objectMapper.readValue(ss, WayPoint.class));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }


        try(FileInputStream fin = new FileInputStream(binInputFile);
            DataInputStream din = new DataInputStream(fin))
        {
            for (WayPoint wp: wayPointList) {
                MatrixLineMap ml = new MatrixLineMap();
                ml.setDistances(new HashMap<>());
                for (int i = 0; i < wayPointList.size(); i++) {
                    double distance = din.readDouble();
                    double time = din.readDouble();
                    ml.getDistances().put(wayPointList.get(i), new TimeDistancePair(time, distance));
                }
                matrix.put(wp, ml);
            }




        }
        System.out.println("\nRestored matrix from: " +
                jsonInputFile +
                " " +
                wayPointList.size() +
                " waypoints");

        System.out.println("loaded in: " + sw.stop().getSeconds() + " s");
    }
}
