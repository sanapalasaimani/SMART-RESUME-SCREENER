import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ResumeParser {

    public static String readTextFile(String filePath) throws IOException {
        if (filePath.toLowerCase().endsWith(".pdf")) {
            try (PDDocument document = PDDocument.load(new File(filePath))) {
                if (!document.isEncrypted()) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
                return "Encrypted PDF cannot be read.";
            }
        }
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static List<File> getResumeFiles(String path) {
        List<File> resumeFiles = new ArrayList<>();
        File fileOrDir = new File(path);
        if (fileOrDir.exists()) {
            if (fileOrDir.isDirectory()) {
                findFilesRecursively(fileOrDir, resumeFiles);
            } else {
                String name = fileOrDir.getName().toLowerCase();
                if (name.endsWith(".txt") || name.endsWith(".pdf")) {
                    resumeFiles.add(fileOrDir);
                }
            }
        }
        return resumeFiles;
    }

    private static void findFilesRecursively(File dir, List<File> resumeFiles) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findFilesRecursively(file, resumeFiles);
                } else {
                    String name = file.getName().toLowerCase();
                    if (name.endsWith(".txt") || name.endsWith(".pdf")) {
                        resumeFiles.add(file);
                    }
                }
            }
        }
    }
}
