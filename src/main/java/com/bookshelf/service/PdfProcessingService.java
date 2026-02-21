package com.bookshelf.service;

import com.bookshelf.exception.PdfProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Service for processing PDF files
 * Handles PDF storage, metadata extraction, thumbnail generation, and file management
 */
@Service
@Slf4j
public class PdfProcessingService {

    @Value("${bookshelf.storage.pdf-directory}")
    private String pdfDirectory;

    @Value("${bookshelf.storage.thumbnail-directory}")
    private String thumbnailDirectory;

    /**
     * Process uploaded PDF file: save, extract metadata, generate thumbnail
     * Uses absolute paths to avoid working directory issues with Tomcat/Spring Boot
     *
     * @param file Uploaded PDF multipart file
     * @param bookId Unique book identifier
     * @return Map containing: pdfPath, thumbnailPath, fileHash, pageCount, title, author
     * @throws PdfProcessingException if PDF processing fails
     */
    public Map<String, Object> processPdf(MultipartFile file, UUID bookId) {
        Map<String, Object> metadata = new HashMap<>();

        try {
            // Resolve to absolute paths so Tomcat's working directory never interferes
            Path pdfDir = Paths.get(pdfDirectory).toAbsolutePath().normalize();
            Path thumbDir = Paths.get(thumbnailDirectory).toAbsolutePath().normalize();

            // Ensure directories exist
            Files.createDirectories(pdfDir);
            Files.createDirectories(thumbDir);

            // Save PDF to disk using Files.copy (safe with absolute paths)
            String pdfFileName = bookId + ".pdf";
            Path pdfPath = pdfDir.resolve(pdfFileName);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, pdfPath, StandardCopyOption.REPLACE_EXISTING);
            }
            metadata.put("pdfPath", pdfPath.toString());

            // Calculate file hash
            String fileHash = calculateFileHash(pdfPath);
            metadata.put("fileHash", fileHash);

            // Extract metadata from PDF
            try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
                // Page count
                int pageCount = document.getNumberOfPages();
                metadata.put("pageCount", pageCount);

                // Document information
                PDDocumentInformation info = document.getDocumentInformation();
                if (info != null) {
                    String title = info.getTitle();
                    String author = info.getAuthor();

                    if (title != null && !title.trim().isEmpty()) {
                        metadata.put("title", title.trim());
                    } else {
                        // Fallback to filename without extension
                        String originalFilename = file.getOriginalFilename();
                        if (originalFilename != null) {
                            metadata.put("title", originalFilename.replaceFirst("[.][^.]+$", ""));
                        }
                    }

                    if (author != null && !author.trim().isEmpty()) {
                        metadata.put("author", author.trim());
                    }
                }

                // Generate thumbnail from first page
                String thumbnailPath = generateThumbnail(document, bookId, thumbDir);
                metadata.put("thumbnailPath", thumbnailPath);

                log.info("Successfully processed PDF: {}", pdfFileName);
            }

            return metadata;

        } catch (IOException e) {
            log.error("Failed to process PDF", e);
            throw new PdfProcessingException("Failed to process PDF file", e);
        }
    }

    /**
     * Generate ultra-high-quality thumbnail from PDF first page
     * Renders at 600 DPI and saves as PNG with lossless compression for maximum sharpness
     * Higher DPI ensures crisp text and sharp images even on high-resolution displays
     *
     * @param document Loaded PDF document
     * @param bookId Book identifier for filename
     * @param thumbDir Thumbnail directory path
     * @return Absolute path to generated thumbnail
     * @throws IOException if thumbnail generation fails
     */
    private String generateThumbnail(PDDocument document, UUID bookId, Path thumbDir) throws IOException {
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            // Render at 600 DPI for ultra-sharp thumbnails (2x previous quality)
            BufferedImage image = renderer.renderImageWithDPI(0, 600);

            // Use PNG format for lossless compression (no quality degradation)
            String thumbnailFileName = bookId + ".png";
            Path thumbnailPath = thumbDir.resolve(thumbnailFileName);

            // Write PNG with maximum quality (lossless compression)
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("PNG");
            if (!writers.hasNext()) {
                throw new IOException("No PNG writer found");
            }

            ImageWriter writer = writers.next();
            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            // PNG is lossless, no compression quality setting needed

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(thumbnailPath.toFile())) {
                writer.setOutput(ios);
                writer.write(null, new javax.imageio.IIOImage(image, null, null), writeParam);
            } finally {
                writer.dispose();
            }

            log.info("Generated thumbnail: {}", thumbnailFileName);
            return thumbnailPath.toString();

        } catch (IOException e) {
            log.error("Failed to generate thumbnail", e);
            throw new PdfProcessingException("Failed to generate thumbnail", e);
        }
    }

    /**
     * Calculate SHA-256 hash of PDF file for duplicate detection
     *
     * @param filePath Path to PDF file
     * @return Hexadecimal hash string
     * @throws PdfProcessingException if hash calculation fails
     */
    private String calculateFileHash(Path filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException | IOException e) {
            log.error("Failed to calculate file hash", e);
            throw new PdfProcessingException("Failed to calculate file hash", e);
        }
    }

    /**
     * Delete PDF and thumbnail files from filesystem
     * Called when a book is deleted or during duplicate cleanup
     *
     * @param pdfPath Path to PDF file (can be null)
     * @param thumbnailPath Path to thumbnail file (can be null)
     */
    public void deleteFiles(String pdfPath, String thumbnailPath) {
        try {
            if (pdfPath != null) {
                Files.deleteIfExists(Paths.get(pdfPath));
                log.info("Deleted PDF file: {}", pdfPath);
            }
            if (thumbnailPath != null) {
                Files.deleteIfExists(Paths.get(thumbnailPath));
                log.info("Deleted thumbnail: {}", thumbnailPath);
            }
        } catch (IOException e) {
            log.error("Failed to delete files", e);
        }
    }

    /**
     * Get PDF file handle for serving to client
     *
     * @param pdfPath Absolute path to PDF file
     * @return File object for PDF
     * @throws PdfProcessingException if file doesn't exist
     */
    public File getPdfFile(String pdfPath) {
        File file = new File(pdfPath);
        if (!file.exists()) {
            throw new PdfProcessingException("PDF file not found at path: " + pdfPath);
        }
        return file;
    }

    /**
     * Get thumbnail file handle for serving to client
     *
     * @param thumbnailPath Absolute path to thumbnail file
     * @return File object for thumbnail
     * @throws PdfProcessingException if file doesn't exist
     */
    public File getThumbnailFile(String thumbnailPath) {
        File file = new File(thumbnailPath);
        if (!file.exists()) {
            throw new PdfProcessingException("Thumbnail file not found at path: " + thumbnailPath);
        }
        return file;
    }
}
