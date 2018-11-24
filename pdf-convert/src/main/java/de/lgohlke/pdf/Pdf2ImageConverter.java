package de.lgohlke.pdf;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@RequiredArgsConstructor
class Pdf2ImageConverter {

    private final String pdfSourceFilename;
    private final String jpegTargetFilename;
    private final ConversionConfig config;

    void convert() throws IOException {
        File pdf = new File(pdfSourceFilename);
        try (PDDocument document = PDDocument.load(pdf)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage subimage = extractSubImage(pdfRenderer, config);
            // suffix in filename will be used as the file format
            ImageIOUtil.writeImage(subimage, jpegTargetFilename, 1, (float) config.compressionQuality / 100);
        }
    }

    private BufferedImage extractSubImage(PDFRenderer pdfRenderer, ConversionConfig config) throws IOException {
        BufferedImage bim = pdfRenderer.renderImageWithDPI(0, config.dpi, ImageType.RGB);

        int x = bim.getWidth() * config.leftPercent / 100;
        int y = bim.getHeight() * config.topPercent / 100;
        int w = bim.getWidth() * config.widthPercent / 100;
        int h = bim.getHeight() * config.heightPercent / 100;

        return bim.getSubimage(x, y, w, h);
    }
}
