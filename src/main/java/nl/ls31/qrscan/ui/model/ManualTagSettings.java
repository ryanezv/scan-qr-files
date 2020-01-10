package nl.ls31.qrscan.ui.model;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import nl.ls31.qrscan.core.QrPdf;

import java.nio.file.Path;

/**
 * This model holds all settings regarding the manual tagging operation.
 *
 * @author Lars Steggink
 */
public class ManualTagSettings {

    private SimpleObjectProperty<Path> pdfPath = new SimpleObjectProperty<>();
    private SimpleStringProperty code = new SimpleStringProperty();

    /**
     * Gets the PDF path. The path may not be valid.
     *
     * @return PDF path
     */
    public final Path getPDFPath() {
        return pdfPath.get();
    }

    /**
     * Sets the PDF path. This does not check the path for validity.
     *
     * @param file PDF path
     */
    public final void setPDFPath(Path file) {
        pdfPath.set(file);
    }

    /**
     * Gets the code to use as the custom file attribute.
     *
     * @return code
     */
    public final String getCode() {
        return code.get();
    }

    /**
     * Sets the code to use as the custom file attribute.
     *
     * @param code the code
     * @throws IllegalArgumentException if the provided code had invalid characters
     */
    public final void setCode(String code) {
        if (!QrPdf.isValidQRCode(code)) {
            throw new IllegalArgumentException("Invalid characters in code.");
        }

        this.code.set(code);
    }
}
