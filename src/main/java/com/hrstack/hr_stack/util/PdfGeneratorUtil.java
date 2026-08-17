package com.hrstack.hr_stack.util;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PdfGeneratorUtil {

    public PdfGeneratorUtil() {
    }

    public static byte[] generatePdf(String htmlContent){
        if (htmlContent == null || htmlContent.isBlank()){
            throw new IllegalArgumentException("htmlContent is null or empty");
        }

        try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()){
            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.withHtmlContent(htmlContent, null);
            builder.toStream(outputStream);
            builder.run();

            return outputStream.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed To generate pdf",e);
        }
    }
}