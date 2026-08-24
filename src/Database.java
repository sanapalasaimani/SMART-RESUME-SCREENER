import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Database {
    private String dbFilePath;

    public Database(String dbFilePath) {
        this.dbFilePath = dbFilePath;
    }

    public void saveCandidates(List<Candidate> candidates) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(dbFilePath, true))) {
            for (Candidate candidate : candidates) {
                // simple CSV format, replacing commas and newlines to avoid breaking format
                String cleanSkills = candidate.getSkills().replace(",", ";").replace("\n", " ");
                String cleanExp = candidate.getExperience().replace(",", ";").replace("\n", " ");
                String cleanEdu = candidate.getEducation().replace(",", ";").replace("\n", " ");
                String cleanJust = candidate.getJustification().replace(",", ";").replace("\n", " ");
                
                writer.write(String.format("%s,%s,%s,%s,%d,%s\n",
                        candidate.getFilename(),
                        cleanSkills,
                        cleanExp,
                        cleanEdu,
                        candidate.getMatchScore(),
                        cleanJust
                ));
            }
            System.out.println("Saved " + candidates.size() + " records to " + dbFilePath);
        } catch (IOException e) {
            System.err.println("Error saving to database: " + e.getMessage());
        }
    }
}
