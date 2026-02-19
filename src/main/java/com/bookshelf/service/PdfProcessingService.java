package com.bookshelf.service;

import com.bookshelf.exception.PdfProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PdfProcessingService {

    @Value("${bookshelf.storage.pdf-directory}")
    private String pdfDirectory;

    @Value("${bookshelf.storage.thumbnail-directory}")
    private String thumbnailDirectory;

    public Map<String, Object> processPdf(MultipartFile file, UUID bookId) {
        Map<String, Object> metadata = new HashMap<>();

        try {
            // Ensure directories exist
            Files.createDirectories(Paths.get(pdfDirectory));
            Files.createDirectories(Paths.get(thumbnailDirectory));

            // Save PDF to disk
            String pdfFileName = bookId + ".pdf";
            Path pdfPath = Paths.get(pdfDirectory, pdfFileName);
            file.transferTo(pdfPath.toFile());
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
                String thumbnailPath = generateThumbnail(document, bookId);
                metadata.put("thumbnailPath", thumbnailPath);

                log.info("Successfully processed PDF: {}", pdfFileName);
            }

            return metadata;

        } catch (IOException e) {
            log.error("Failed to process PDF", e);
            throw new PdfProcessingException("Failed to process PDF file", e);
        }
    }

    private String generateThumbnail(PDDocument document, UUID bookId) throws IOException {
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, 150); // First page at 150 DPI

            String thumbnailFileName = bookId + ".jpg";
            Path thumbnailPath = Paths.get(thumbnailDirectory, thumbnailFileName);

            ImageIO.write(image, "JPEG", thumbnailPath.toFile());

            log.info("Generated thumbnail: {}", thumbnailFileName);
            return thumbnailPath.toString();

        } catch (IOException e) {
            log.error("Failed to generate thumbnail", e);
            throw new PdfProcessingException("Failed to generate thumbnail", e);
        }
    }

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

    public File getPdfFile(String pdfPath) {
        File file = new File(pdfPath);
        if (!file.exists()) {
            throw new PdfProcessingException("PDF file not found at path: " + pdfPath);
        }
        return file;
    }

    public File getThumbnailFile(String thumbnailPath) {
        File file = new File(thumbnailPath);
        if (!file.exists()) {
            throw new PdfProcessingException("Thumbnail file not found at path: " + thumbnailPath);
        }
        return file;
    }
}
