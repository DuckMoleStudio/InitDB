import entity.BusStop;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.locationtech.jts.geom.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import static service.BusStopService.addBusStop;

// DEPRECATED

public class ImportBusStopsXLS {
    public static void main(String[] args) {

        int count=0;
        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        try(
                FileInputStream fis = new FileInputStream("c:/matrix/stops01.xls");
                HSSFWorkbook workbook = new HSSFWorkbook(fis)) {
            // Get first sheet from the workbook
            HSSFSheet sheet = workbook.getSheetAt(0);

            // Get iterator to all the rows in current sheet
            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next(); // skip first row with headers
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Create entity
                final BusStop busStop = new BusStop();

                // Get significant fields

                busStop.setId(Integer.parseInt(row.getCell(0).getStringCellValue()));
                busStop.setName(row.getCell(8).getStringCellValue());

                double lon = Double.parseDouble(row.getCell(2).getStringCellValue());
                double lat = Double.parseDouble(row.getCell(3).getStringCellValue());

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
