import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class SmartResumeScreener {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("         SMART RESUME SCREENER           ");
        System.out.println("=========================================");

        // Load API Key
        Map<String, String> env = EnvParser.loadEnv(".env");
        String apiKey = env.get("GROQ_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: GROQ_API_KEY not found in .env file.");
            return;
        }

        Scanner scanner = new Scanner(System.in);

        // Get Job Description
        System.out.print("Enter path to Job Description file (.txt): ");
        String jdPath = scanner.nextLine().trim();
        String jobDescription;
        try {
            jobDescription = ResumeParser.readTextFile(jdPath);
        } catch (Exception e) {
            System.err.println("Error reading Job Description: " + e.getMessage());
            return;
        }

        // Get Resumes Directory
        System.out.print("Enter directory path containing Resumes (.txt): ");
        String resumesDir = scanner.nextLine().trim();
        List<File> resumeFiles = ResumeParser.getResumeFiles(resumesDir);
        
        if (resumeFiles.isEmpty()) {
            System.out.println("No .txt resumes found in the specified directory.");
            return;
        }

        System.out.println("\nFound " + resumeFiles.size() + " resumes. Processing...");

        LLMService llmService = new LLMService(apiKey);
        List<Candidate> candidates = new ArrayList<>();

        for (File file : resumeFiles) {
            System.out.println("Parsing: " + file.getName() + " ...");
            try {
                String resumeText = ResumeParser.readTextFile(file.getAbsolutePath());
                Candidate candidate = llmService.analyzeResume(file.getName(), resumeText, jobDescription);
                candidates.add(candidate);
            } catch (Exception e) {
                System.err.println("Error processing " + file.getName() + ": " + e.getMessage());
            }
        }

        // Sort candidates by match score descending
        candidates.sort(Comparator.comparingInt(Candidate::getMatchScore).reversed());

        // Save to Database
        Database db = new Database("database.csv");
        db.saveCandidates(candidates);

        // Display results
        System.out.println("\n=========================================");
        System.out.println("         SHORTLISTED CANDIDATES          ");
        System.out.println("=========================================\n");
        
        for (Candidate c : candidates) {
            System.out.println(c.toString());
        }
        
        System.out.println("Processing complete. Data saved to database.csv.");
        scanner.close();
    }
}
