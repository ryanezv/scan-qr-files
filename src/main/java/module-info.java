module qrscan {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.zxing;
    requires java.desktop;
    requires org.apache.pdfbox;
    requires com.google.zxing.javase;

    opens nl.ls31.qrscan to javafx.fxml;
    exports nl.ls31.qrscan;
}