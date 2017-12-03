import java.io.*;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.tess4j.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class TesseractExample {

    public static void main(String[] args) {
        String result = new String();
        File imageFile = new File("test1.png");
        ITesseract instance = new Tesseract();
        FileOutputStream out = null;
        Map <String, String>  Dates = new HashMap<String, String>();
        Integer i = new Integer(0);

        try {
            out = new FileOutputStream("result.xls");
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }

        Workbook wb = new HSSFWorkbook();
        Sheet s = wb.createSheet("Dátumok");
        Row r = null;
        Cell c = null;

        instance.setDatapath("C:\\Users\\Gabe\\IdeaProjects\\example\\src\\main\\tessdata");
        instance.setLanguage("hun");

        try {
            result = instance.doOCR(imageFile);
            //System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }

        String pCause = "([Kk]elt)\\.*\\:*\\s*(\\d{4}\\-\\d{2}\\-\\d{2})";
        Pattern rCause = Pattern.compile(pCause);
        Matcher mCause = rCause.matcher(result);
        if (mCause.find()) {
            System.out.println(mCause.group(1).toLowerCase() + ": " + mCause.group(2));
            Dates.put(mCause.group(1).toLowerCase(), mCause.group(2));
        } else {
            System.out.println("Nem talált keltezési dátumot!");
        }

        String pFulfillment = "([Tt]eljes[ií]tés)\\.*\\:*\\s*(\\d{4}\\-\\d{2}\\-\\d{2})";
        Pattern rFulfillment = Pattern.compile(pFulfillment);
        Matcher mFulfillment = rFulfillment.matcher(result);
        if (mFulfillment.find()) {
            System.out.println(mFulfillment.group(1).toLowerCase() + ": " + mFulfillment.group(2));
            Dates.put(mFulfillment.group(1).toLowerCase(), mFulfillment.group(2));
        } else {
            System.out.println("Nem talált teljesítési dátumot!");
        }

        String pDeadline = "([Hh]atáridő)\\.*\\:*\\s*(\\d{4}\\-\\d{2}\\-\\d{2})";
        Pattern rDeadline = Pattern.compile(pDeadline);
        Matcher mDeadline = rDeadline.matcher(result);
        if (mDeadline.find()) {
            System.out.println(mDeadline.group(1).toLowerCase() + ": " + mDeadline.group(2));
            Dates.put(mDeadline.group(1).toLowerCase(), mDeadline.group(2));
        } else {
            System.out.println("Nem talált fizetési határidő dátumot!");
        }

        for (String key : Dates.keySet()) {
            r = s.createRow(i);
            c = r.createCell(0);
            c.setCellValue(key);
            c = r.createCell(1);
            c.setCellValue(Dates.get(key));
            i++;
        }

        try {
            wb.write(out);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        try {
            out.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
