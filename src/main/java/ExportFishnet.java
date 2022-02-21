import entity.FishnetCell2;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static service.FishnetCell2Service.listFishnetCells2;

public class ExportFishnet {
    public static void main(String[] args) {
        List<FishnetCell2> cells = listFishnetCells2();

        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("Population");

        int rownum = 0;
        Cell cell;
        Row row;

        for(FishnetCell2 fncell: cells)
        {
            row = sheet.createRow(rownum);
            rownum++;

            // id
            cell = row.createCell(0, CellType.NUMERIC);
            cell.setCellValue(fncell.getId());
            // cnt_home
            cell = row.createCell(1, CellType.NUMERIC);
            cell.setCellValue(fncell.getHome());
            // cnt_work
            cell = row.createCell(2, CellType.NUMERIC);
            cell.setCellValue(fncell.getWork());
        }

        File file = new File("c:/matrix/fishnet.xls");
        file.getParentFile().mkdirs();

        FileOutputStream outFile = null;
        try {
            outFile = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            workbook.write(outFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Created file: " + file.getAbsolutePath());
    }
}
