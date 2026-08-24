import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class LLMService {
    private String apiKey;
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";

    public LLMService(String apiKey) {
        this.apiKey = apiKey;
    }

    public Candidate analyzeResume(String filename, String resumeText, String jobDescription) {
        String prompt = "Compare the following resume with this job description. " +
                "Extract structured data: skills, experience, education. " +
                "Compute a match score between candidate and job description (1-10). " +
                "Provide a justification. " +
                "Return exactly a JSON object (and nothing else) with exactly these keys: 'skills' (string), 'experience' (string), 'education' (string), 'matchScore' (integer), and 'justification' (string). " +
                "IMPORTANT: All values except matchScore MUST be plain strings (not arrays or objects). Join multiple items with commas. Do NOT use placeholders like '[...]', '...', or 'etc' - provide the full extracted text." +
                "\n\nJob Description:\n" + jobDescription +
                "\n\nResume:\n" + resumeText;

        String escapedPrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
        
        String jsonPayload = "{"
                + "\"model\": \"qwen/qwen3.6-27b\","
                + "\"messages\": [{\"role\": \"user\", \"content\": \"" + escapedPrompt + "\"}],"
                + "\"temperature\": 0.1"
                + "}";

        try {
            HttpClient client = HttpClient.newBuilder()
                    .version(HttpClient.Version.HTTP_2)
                    .connectTimeout(Duration.ofSeconds(20))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .timeout(Duration.ofMinutes(1))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return parseJsonResponse(filename, response.body());
            } else {
                System.err.println("API Error: " + response.statusCode() + " - " + response.body());
                return new Candidate(filename, "Error", "Error", "Error", 0, "API Error");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Candidate(filename, "Error", "Error", "Error", 0, "Exception: " + e.getMessage());
        }
    }

    private Candidate parseJsonResponse(String filename, String json) {
        // Very basic JSON extraction since no external libraries are allowed
        String content = extractJsonValue(json, "\"content\"");
        if (content == null) {
            return new Candidate(filename, "Parse Error", "", "", 0, "Failed to find content");
        }
        
        // Remove extra escaping from the content string returned by LLM
        content = content.replace("\\n", "\n").replace("\\\"", "\"");
        
        String skills = extractJsonValue(content, "\"skills\"");
        String experience = extractJsonValue(content, "\"experience\"");
        String education = extractJsonValue(content, "\"education\"");
        String scoreStr = extractJsonValue(content, "\"matchScore\"");
        String justification = extractJsonValue(content, "\"justification\"");
        
        int matchScore = 0;
        try {
            if (scoreStr != null) {
                matchScore = Integer.parseInt(scoreStr.replaceAll("[^0-9]", ""));
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        
        return new Candidate(
                filename, 
                skills != null ? skills : "N/A", 
                experience != null ? experience : "N/A", 
                education != null ? education : "N/A", 
                matchScore, 
                justification != null ? justification : "N/A"
        );
    }

    private String extractJsonValue(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return null;
        
        int colonIndex = json.indexOf(":", keyIndex);
        if (colonIndex == -1) return null;
        
        // Find the start of the value
        int i = colonIndex + 1;
        while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
            i++;
        }
        
        if (i >= json.length()) return null;
        
        if (json.charAt(i) == '"') {
            // It's a string value
            int start = i + 1;
            int end = start;
            while (end < json.length()) {
                if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') {
                    break;
                }
                end++;
            }
            return json.substring(start, end);
        } else if (json.charAt(i) == '[') {
            // It's an array
            int start = i;
            int end = start;
            int bracketCount = 0;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == '[') bracketCount++;
                else if (c == ']') bracketCount--;
                end++;
                if (bracketCount == 0) break;
            }
            return json.substring(start, end);
        } else {
            // It's a number or boolean (or nested object, but we keep it simple)
            int start = i;
            int end = start;
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}' && !Character.isWhitespace(json.charAt(end))) {
                end++;
            }
            return json.substring(start, end);
        }
    }
}
