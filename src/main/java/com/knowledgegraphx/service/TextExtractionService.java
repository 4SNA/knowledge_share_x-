package com.knowledgegraphx.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TextExtractionService {

    private static final Logger log = LoggerFactory.getLogger(TextExtractionService.class);

    public String extractText(MultipartFile file) throws IOException {
        String fileName = file.getOriginalFilename();
        if (fileName == null) throw new IOException("File name is null");

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        return switch (extension) {
            case "pdf" -> extractFromPdf(file);
            case "docx" -> extractFromDocx(file);
            case "csv" -> extractFromCsv(file);
            case "txt", "md" -> extractFromText(file);
            default -> throw new IOException("Unsupported file type: " + extension);
        };
    }

    private String extractFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.info("Extracted {} characters from PDF: {}", text.length(), file.getOriginalFilename());
            return text;
        }
    }

    private String extractFromDocx(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder sb = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                sb.append(paragraph.getText()).append("\n");
            }
            String text = sb.toString();
            log.info("Extracted {} characters from DOCX: {}", text.length(), file.getOriginalFilename());
            return text;
        }
    }

    private String extractFromCsv(MultipartFile file) throws IOException {
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {
            List<String[]> rows = csvReader.readAll();
            StringBuilder sb = new StringBuilder();
            for (String[] row : rows) {
                sb.append(String.join(", ", row)).append("\n");
            }
            String text = sb.toString();
            log.info("Extracted {} characters from CSV: {}", text.length(), file.getOriginalFilename());
            return text;
        } catch (CsvException e) {
            throw new IOException("Failed to parse CSV file", e);
        }
    }

    private String extractFromText(MultipartFile file) throws IOException {
        String text = new String(file.getBytes());
        log.info("Extracted {} characters from TEXT: {}", text.length(), file.getOriginalFilename());
        return text;
    }
}
