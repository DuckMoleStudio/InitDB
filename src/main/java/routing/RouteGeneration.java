package routing;

import entity.Version;
import routing.entity.WayPoint;
import routing.entity.result.Result;
import routing.entity.storage.MatrixLineMap;
import routing.fileManagement.LoadMatrixB;
import routing.fileManagement.WriteGH;
import routing.service.greedyAlgos.LinkV00;

import java.sql.Date;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static routing.service.ExportDB.ExportRoutes;
import static service.VersionService.getVersionById;
import static service.VersionService.updateVersion;

public class RouteGeneration {
    public static void main(String[] args) {

        // ----- CONTROLS (set before use) -----------
        String jsonInputFile = "C:\\matrix\\data\\zao1_mtrx.json";
        String binInputFile = "C:\\matrix\\data\\zao1_mtrx.bin";

        //OSM data
        String osmFile = "C:/matrix/RU-MOW.osm.pbf";
        String dir = "local/graphhopper";


        String algo = "V00"; // algo to perform
        LocalTime workStart = LocalTime.parse("06:00");
        LocalTime workEnd = LocalTime.parse("22:00");
        boolean isGood = false; // with access from R curbside, for GPX. True for "good" files, false for rest
        int capacity = 50;
        int iterations = 200; // these 2 for jsprit algo

        String urlOutputFile = "C:\\matrix\\data\\out\\zao.txt";
        String arrOutputFile = "C:\\Users\\User\\Documents\\GD\\a2.txt";
        String outDir = "C:\\Users\\User\\Documents\\GD\\tracks\\a3";
        // ----- CONTROLS END ---------


        // ------- RESTORE MATRIX FROM JSON FILE ---------

        List<WayPoint> wayPointList = new ArrayList<>();
        Map<WayPoint, MatrixLineMap> matrix = new HashMap<>();
        try {
            LoadMatrixB.restoreB(wayPointList, matrix, jsonInputFile,binInputFile);
        } catch (Exception e) {
            System.out.println("Invalid data file provided");
            return;
        }


        // ------- RUN ALGO -------

            Result rr = new Result();
            switch(algo)
            {
                case "V00": rr = LinkV00.Calculate(wayPointList, matrix); break;
                  }


        // ----- SAVE RESULTS FOR VISUALISATION ------

            WriteGH.write(rr, urlOutputFile);
            //WriteGPX.write(osmFile, dir, outDir, rr, isGood);
            //WriteArrivals.write(rr, arrOutputFile);

        // ----- SAVE TO MODEL IN POSTGRES -----

      /*  Version version = new Version();
        version.setDesc("Первый алгоритм генерации");
        version.setDate(Date.valueOf("2022-3-26"));
        updateVersion(version);
               */
        ExportRoutes(6,rr,matrix,osmFile,dir);

        }

}
