package com.leo;

import com.aspose.pdf.Document;
import com.aspose.pdf.devices.PngDevice;
import com.aspose.pdf.devices.Resolution;
import org.junit.Test;

public class PdfToImage {

    @Test
    public void pdfToImage() throws Exception {
        // For complete examples and data files, please go to https://github.com/aspose-pdf/Aspose.Pdf-for-Java
// Open document
        Document pdfDocument = new Document("input.pdf");

// Loop through all the pages of PDF file
        for (int pageCount = 1; pageCount <= pdfDocument.getPages().size(); pageCount++) {
            // Create stream object to save the output image
            java.io.OutputStream imageStream = new java.io.FileOutputStream("Converted_Image" + pageCount + ".png");

            // Create Resolution object
            Resolution resolution = new Resolution(300);
            // Create PngDevice object with particular resolution
            PngDevice pngDevice = new PngDevice(resolution);
            // Convert a particular page and save the image to stream
            pngDevice.process(pdfDocument.getPages().get_Item(pageCount), imageStream);

            // Close the stream
            imageStream.close();
        }
    }
}
