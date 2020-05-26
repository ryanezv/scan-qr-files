package nl.ls31.qrscan.core;

import com.google.zxing.NotFoundException;
import javafx.concurrent.Task;
import org.tinylog.Logger;

import java.awt.*;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * In this Task, PDF files in the input directory are scanned recursively (at a specified page), results are aggregated
 * and finally reported as an CSV file within the input directory.
 * <p>
 * The user should predefine the page where the QR code can be found, as this will make scanning for QR codes
 * tremendously more efficient.
 *
 * @author Lars Steggink
 */
public class ScanTask extends Task<List<SingleResult>> {
    final static private String LSEP = System.lineSeparator();
    protected final Path inputDir;
    private final int qrCodePage;
    private final boolean writeFileAttributes;
    private final boolean useFileAttributes;
    private final boolean openLogFile;

    /**
     * @param inputDir            Input directory with PDF files.
     * @param qrCodePage          Page where QR codes are expected.
     * @param writeFileAttributes whether to write a custom file attribute after a QR code has been recognised
     * @param useFileAttributes   whether to use the custom file attribute to get stored QR codes instead of (slow)
     *                            scanning
     * @param openLogFile         whether to open the CSV log file at the end.
     */
    public ScanTask(Path inputDir, int qrCodePage, boolean useFileAttributes, boolean writeFileAttributes, boolean openLogFile) {
        this.inputDir = inputDir;
        this.qrCodePage = qrCodePage;
        this.useFileAttributes = useFileAttributes;
        this.writeFileAttributes = writeFileAttributes;
        this.openLogFile = openLogFile;
    }

    /**
     * Iterates over every PDF file and tries to find the QR code.
     */
    @Override
    protected List<SingleResult> call() {
        List<QrPdf> pdfs = findInputFiles(inputDir);
        List<SingleResult> results = scanInputFiles(pdfs);
        logResults(results, inputDir);
        return results;
    }

    /**
     * Logs the results by logging to a CSV file.
     * <p>
     * TODO Move logging outside of task
     *
     * @param results Results from scanning.
     * @param dir     Directory to save CSV file into.
     */
    protected void logResults(List<SingleResult> results, Path dir) {
        // Create a time stamp for the log file name, then log in that file.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
        String timestamp = sdf.format(Calendar.getInstance().getTime());
        Path logFile = dir.resolve("ScanResults_QRScan_" + timestamp + ".csv");
        try {
            CsvLogWriter.writeLogFile(results, logFile);
            Logger.info("Results were logged to CSV file: " + logFile.getFileName() + ".");
            if (openLogFile) {
                Desktop.getDesktop().open(logFile.toFile());
            }
        } catch (IOException e) {
            Logger.error(e, "Unable to log results in CSV file.");
        }
    }

    /**
     * Lists all PDF files in a specific directory.
     *
     * @param inputDir Path to directory.
     * @return List of PDF files (no guarantees regarding available QR codes).
     */
    protected List<QrPdf> findInputFiles(Path inputDir) {
        List<QrPdf> allFiles = new ArrayList<>();

        SimpleFileVisitor<Path> pdfFileVisitor = new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {
                // Convert path to file
                if (filePath.toString().toLowerCase().endsWith(".pdf")) {
                    allFiles.add(new QrPdf(filePath));
                }
                return FileVisitResult.CONTINUE;
            }
        };

        try {
            Files.walkFileTree(inputDir, pdfFileVisitor);
        } catch (IOException e) {
            Logger.error(e, "!Unable to read PDF file.");
        }
        return allFiles;
    }

    /**
     * Scans list of input files for QR codes.
     *
     * @param inputFiles List of files to scan for QR codes.
     * @return Results from scanning the input files.
     */
    protected List<SingleResult> scanInputFiles(List<QrPdf> inputFiles) {
        int fileCount = inputFiles.size();
        int success = 0;
        int failed = 0;
        int current = 0;
        Logger.info("New scan initiated." + LSEP + "  Input directory: " + inputDir.getFileName() + LSEP
                + "  Scanning page:   " + qrCodePage + LSEP + "  Number of files: " + fileCount);

        // Start the loop through all files.
        List<SingleResult> results = new ArrayList<>();
        for (QrPdf pdf : inputFiles) {
            updateProgress(++current, fileCount);
            Logger.info("Now scanning file " + pdf.getPath().getFileName() + ".");

            try {
                String qrCode = pdf.getQRCode(qrCodePage, useFileAttributes, writeFileAttributes);
                Logger.info("Found QR code " + qrCode + " in " + pdf.getPath().getFileName() + ".");
                results.add(new SingleResult(pdf, SingleResult.ResultStatus.QR_CODE_FOUND, qrCodePage, qrCode));
                success++;
            } catch (IOException e) {
                Logger.warn(e, "!Unable to access " + pdf.getPath().getFileName() + " or page not found.");
                results.add(new SingleResult(pdf, SingleResult.ResultStatus.NO_FILE_ACCESS, qrCodePage, ""));
                failed++;
            } catch (NotFoundException e) {
                Logger.warn(e, "!Unable to find QR code at specified page in " + pdf.getPath().getFileName() + ".");
                results.add(new SingleResult(pdf, SingleResult.ResultStatus.NO_QR_CODE, qrCodePage, ""));
                failed++;
            }
        }

        String summaryMessage = "Summary: scanned " + fileCount + " files: " + success + " successful, " + failed + " unsuccessful.";
        Logger.info(summaryMessage);
        updateMessage(summaryMessage);
        return results;
    }
}