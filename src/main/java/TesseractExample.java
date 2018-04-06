import net.sourceforge.tess4j.*;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TesseractExample {

    public static void main(String[] args) throws IOException {
        Map <String, String> Dates = new HashMap<String, String>();

        String scanDir = getInvoicesDirectory();
        System.out.println("Scandir: " + scanDir);

        File imageFile = new File(scanDir + "\\" + "test1.png");
        System.out.println("Input file: " + imageFile);

        BufferedImage imageBuff = ImageIO.read(imageFile);

        for (float f=1.0f; f < 1.5f; f=f+0.05f) {
            adjustImage(imageBuff, f);

            Dates = getDates(tessImage(imageBuff));
            if (Dates != null) {
                break;
            }
        }

        createWorkbook(Dates);
    }

    private static void createWorkbook(Map<String, String> Dates) throws IOException {
        FileOutputStream out = null;
        Integer i = 0;

        String scanDir = getInvoicesDirectory();

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

    private static String tessImage (BufferedImage imageBuff) {
        ITesseract instance = new Tesseract();
        String invoiceText = new String();

        instance.setDatapath("src/main/resources");
        instance.setLanguage("hun");
        instance.setPageSegMode(3);

        try {
            invoiceText = instance.doOCR(imageBuff);
            System.out.println(invoiceText);
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
        }

        return invoiceText;
    }

    private static Map <String, String> getDates(String invoiceText) {
        Map<String, String> Dates = new HashMap<String, String>();
        String[] patternArray = new String[6];
        boolean column = false;
        String formattedDate;

        String patternCause = ".*([Kk]elt).*";
        String patternFulfillment = ".*([Tt]elj(?:es[ií]t[eé]s)?).*";
        String patternDeadline = ".*([Hh]at\\.?(?:[aá]r)?id\\.?[oóöő]?|[Ee]sed[eé]kess[eé]g).*";
        String patternSeparator = "[.:\\s]*";
        String patternDate = "(\\d{4}.*\\d{2}.*\\d{2})";
        String patternDateColumn = patternDate + patternSeparator + patternDate + patternSeparator + patternDate;

        patternArray[0] = patternCause + patternSeparator + patternFulfillment + patternSeparator + patternDeadline;
        patternArray[1] = patternCause + patternSeparator + patternDeadline + patternSeparator + patternFulfillment;
        patternArray[2] = patternFulfillment + patternSeparator + patternDeadline + patternSeparator + patternCause;
        patternArray[3] = patternFulfillment + patternSeparator + patternCause + patternSeparator + patternDeadline;
        patternArray[4] = patternDeadline + patternSeparator + patternFulfillment + patternSeparator + patternCause;
        patternArray[5] = patternDeadline + patternSeparator + patternCause + patternSeparator + patternFulfillment;

        for (String patternString : patternArray) {
            Pattern columnPattern = Pattern.compile(patternString);
            Matcher columnMatcher = columnPattern.matcher(invoiceText);

            if (columnMatcher.find()) {
                Integer lineNumberPattern = getLine(invoiceText, columnMatcher.start());

                Pattern columnDatePattern = Pattern.compile(patternDateColumn);
                Matcher columnDateMatcher = columnDatePattern.matcher(invoiceText);

                if (columnDateMatcher.find()) {
                    Integer lineNumberDates = getLine(invoiceText, columnDateMatcher.start());
                    System.out.println("Oszlop dátum minta találat: " + lineNumberPattern + "-" + lineNumberDates + " sor.");
                    for (int j = 1; j < 4; j++) {
                        formattedDate=formatDate(columnDateMatcher.group(j));
                        System.out.println(columnMatcher.group(j).toLowerCase() + ": " + formattedDate);
                        Dates.put(columnMatcher.group(j).toLowerCase(), formattedDate);
                    }

                    column = true;
                }
            }
        }

        if (!column) {
            String pCause = patternCause + patternSeparator + patternDate;
            Pattern rCause = Pattern.compile(pCause);
            Matcher mCause = rCause.matcher(invoiceText);
            if (mCause.find()) {
                formattedDate=formatDate(mCause.group(2));
                System.out.println(mCause.group(1).toLowerCase() + ": " + formattedDate);
                Dates.put(mCause.group(1).toLowerCase(), formattedDate);
            } else {
                System.out.println("Nem talált keltezési dátumot!");
                return null;
            }

            String pFulfillment = patternFulfillment + patternSeparator + patternDate;
            Pattern rFulfillment = Pattern.compile(pFulfillment);
            Matcher mFulfillment = rFulfillment.matcher(invoiceText);
            if (mFulfillment.find()) {
                formattedDate=formatDate(mFulfillment.group(2));
                System.out.println(mFulfillment.group(1).toLowerCase() + ": " + formattedDate);
                Dates.put(mFulfillment.group(1).toLowerCase(), formattedDate);
            } else {
                System.out.println("Nem talált teljesítési dátumot!");
                return null;
            }

            String pDeadline = patternDeadline + patternSeparator + patternDate;
            Pattern rDeadline = Pattern.compile(pDeadline);
            Matcher mDeadline = rDeadline.matcher(invoiceText);
            if (mDeadline.find()) {
                formattedDate=formatDate(mDeadline.group(2));
                System.out.println(mDeadline.group(1).toLowerCase() + ": " + formattedDate);
                Dates.put(mDeadline.group(1).toLowerCase(), formattedDate);
            } else {
                System.out.println("Nem talált fizetési határidő dátumot!");
                return null;
            }
        }

        return Dates;
    }

    private static String formatDate(String unformattedDate) {
        String formattedDate = new String();
        String patternDate = "(\\d{4}).*(\\d{2}).*(\\d{2})";
        Pattern pDate = Pattern.compile(patternDate);
        Matcher mDate = pDate.matcher(unformattedDate);
        if (mDate.find()) {
            formattedDate = mDate.group(1) + "-" +  mDate.group(2) + "-" +  mDate.group(3);
        } else {
            System.err.println("Nem formázható a dátum!");
            System.exit(1);
        }
        return formattedDate;
    }

    private static BufferedImage adjustImage(BufferedImage imageBuff, float adjustRatio) throws IOException {
        System.out.println("Adjust ratio: " + adjustRatio);
        RescaleOp op = new RescaleOp(adjustRatio, 0, null);
        op.filter(imageBuff, imageBuff);

        String scanDir = getInvoicesDirectory();
        File outputfile = new File(scanDir + "\\" + "modified.jpg");

        try {
            ImageIO.write(imageBuff, "jpg", outputfile);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }

        return imageBuff;
    }
}
