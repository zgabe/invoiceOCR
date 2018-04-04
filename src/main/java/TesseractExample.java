import net.sourceforge.tess4j.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TesseractExample {

    public static void main(String[] args) throws IOException {
        String result = new String();
        String scanDir;
        ITesseract instance = new Tesseract();
        FileOutputStream out = null;
        Map <String, String>  Dates = new HashMap<String, String>();
        Integer i = 0;

        instance.setDatapath("src/main/resources");
        instance.setLanguage("hun");

        scanDir = getInvoicesDirectory();
        System.out.println("Scandir: " + scanDir);

        File imageFile = new File(scanDir + "\\" + "test2.png");

        System.out.println("Input file: " + imageFile);

        try {
            result = instance.doOCR(imageFile);
            System.out.println(result);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }

        String pCause = "([Kk]elte?)\\.*\\:*\\s*(\\d{4}[-.]\\d{2}[-.]\\d{2})";
        Pattern rCause = Pattern.compile(pCause);
        Matcher mCause = rCause.matcher(result);
        if (mCause.find()) {
            System.out.println(mCause.group(1).toLowerCase() + ": " + mCause.group(2));
            //System.out.println(getLine(result, mCause.start()));
            Dates.put(mCause.group(1).toLowerCase(), mCause.group(2));
        } else {
            System.out.println("Nem talált keltezési dátumot!");
        }

        String pFulfillment = "([Tt]elj(es[ií]tés)?)\\.*\\:*\\s*(\\d{4}[-.]\\d{2}[-.]\\d{2})";
        Pattern rFulfillment = Pattern.compile(pFulfillment);
        Matcher mFulfillment = rFulfillment.matcher(result);
        if (mFulfillment.find()) {
            System.out.println(mFulfillment.group(1).toLowerCase() + ": " + mFulfillment.group(3));
            Dates.put(mFulfillment.group(1).toLowerCase(), mFulfillment.group(3));
        } else {
            System.out.println("Nem talált teljesítési dátumot!");
        }

        String pDeadline = "([Hh]atáridő|[Ee]sedékesség)\\.*\\:*\\s*(\\d{4}[-.]\\d{2}[-.]\\d{2})";
        Pattern rDeadline = Pattern.compile(pDeadline);
        Matcher mDeadline = rDeadline.matcher(result);
        if (mDeadline.find()) {
            System.out.println(mDeadline.group(1).toLowerCase() + ": " + mDeadline.group(2));
            Dates.put(mDeadline.group(1).toLowerCase(), mDeadline.group(2));
        } else {
            System.out.println("Nem talált fizetési határidő dátumot!");
        }

        try {
            out = new FileOutputStream(scanDir + "\\" + "result.xls");
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }

        Workbook wb = new HSSFWorkbook();
        Sheet s = wb.createSheet("Dátumok");
        Row r;
        Cell c;

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

    private static int getLine(String result, int start) {
        int line = 1;
        Pattern pattern = Pattern.compile("\n");
        Matcher matcher = pattern.matcher(result);
        matcher.region(0, start);

        while(matcher.find()) {
            line++;
        }

        return(line);
    }

    private static String getInvoicesDirectory() throws IOException {

        String scanDir;
        Properties mainProperties = new Properties();
        FileInputStream file = new FileInputStream("config.properties");
        mainProperties.load(file);
        file.close();

        scanDir = mainProperties.getProperty("scanDir");

        return scanDir;
    }
}
