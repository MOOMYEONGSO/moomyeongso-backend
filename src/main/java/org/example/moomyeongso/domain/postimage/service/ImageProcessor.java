package org.example.moomyeongso.domain.postimage.service;

import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.common.exception.CustomException;
import org.example.moomyeongso.common.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

@Component
@RequiredArgsConstructor
public class ImageProcessor {

    private static final int MAX_PIXELS = 25_000_000;
    private static final String THUMBNAIL_FORMAT = "jpg";

    private final PostImageProperties properties;

    public ProcessedImage process(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            ImageFormat format = ImageFormat.detect(bytes, file.getContentType());
            BufferedImage original = readImage(bytes);

            byte[] thumbnail = createThumbnail(original);
            return new ProcessedImage(bytes, thumbnail, format, original.getWidth(), original.getHeight());
        } catch (CustomException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
    }

    private BufferedImage readImage(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new CustomException(ErrorCode.INVALID_IMAGE);
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                validateDimensions(reader.getWidth(0), reader.getHeight(0));
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw new CustomException(ErrorCode.INVALID_IMAGE);
                }
                return image;
            } finally {
                reader.dispose();
            }
        }
    }

    private void validateDimensions(int width, int height) {
        long pixels = (long) width * height;
        if (pixels <= 0 || pixels > MAX_PIXELS) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }
    }

    private byte[] createThumbnail(BufferedImage original) throws IOException {
        int maxSide = properties.getThumbnailSize();
        double ratio = Math.min(1.0, (double) maxSide / Math.max(original.getWidth(), original.getHeight()));
        int width = Math.max(1, (int) Math.round(original.getWidth() * ratio));
        int height = Math.max(1, (int) Math.round(original.getHeight() * ratio));

        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, width, height);
            graphics.drawImage(original, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }

        return writeJpeg(thumbnail);
    }

    private byte[] writeJpeg(BufferedImage image) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(THUMBNAIL_FORMAT);
        if (!writers.hasNext()) {
            throw new CustomException(ErrorCode.INVALID_IMAGE);
        }

        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();
             ImageOutputStream imageOutput = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(imageOutput);
            ImageWriteParam params = writer.getDefaultWriteParam();
            if (params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(0.85f);
            }
            writer.write(null, new IIOImage(image, null, null), params);
            return output.toByteArray();
        } finally {
            writer.dispose();
        }
    }
}
