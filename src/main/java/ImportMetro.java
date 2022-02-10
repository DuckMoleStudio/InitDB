
import entity.Metro;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

import static service.MetroService.addMetro;


public class ImportMetro {
    public static void main(String[] args) {
        int count=0;
        long startTime = System.currentTimeMillis();
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(),4326);

        try(
                FileInputStream fis = new FileInputStream("c:/matrix/metro.xls");
                HSSFWorkbook workbook = new HSSFWorkbook(fis)) {
            // Get first sheet from the workbook
            HSSFSheet sheet = workbook.getSheetAt(0);

            // Get iterator to all the rows in current sheet
            Iterator<Row> rowIterator = sheet.iterator();
            rowIterator.next(); // skip first row with headers
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();

                // Create entity
                final Metro metro = new Metro();

                // Get significant fields

                metro.setId(Integer.parseInt(row.getCell(0).getStringCellValue()));
                metro.setName(row.getCell(1).getStringCellValue());
                metro.setStation(row.getCell(8).getStringCellValue());
                metro.setLine(row.getCell(9).getStringCellValue());

                double lon = Double.parseDouble(row.getCell(5).getStringCellValue());
                double lat = Double.parseDouble(row.getCell(6).getStringCellValue());

                Coordinate pointCoord = new Coordinate(lon,lat);
                Point point = gf.createPoint(pointCoord);

                metro.setGeom(point);

                addMetro(metro);


                count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }



        System.out.printf("\n\n===== Loaded %d metro entrances in %d seconds ======\n\n"
                , count, (System.currentTimeMillis()-startTime)/1000);

    }
}
