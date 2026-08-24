public class Candidate {
    private String filename;
    private String skills;
    private String experience;
    private String education;
    private int matchScore;
    private String justification;

    public Candidate(String filename, String skills, String experience, String education, int matchScore, String justification) {
        this.filename = filename;
        this.skills = skills;
        this.experience = experience;
        this.education = education;
        this.matchScore = matchScore;
        this.justification = justification;
    }

    public String getFilename() { return filename; }
    public String getSkills() { return skills; }
    public String getExperience() { return experience; }
    public String getEducation() { return education; }
    public int getMatchScore() { return matchScore; }
    public String getJustification() { return justification; }

    @Override
    public String toString() {
        return "Candidate: " + filename + "\n" +
               "Score: " + matchScore + "/10\n" +
               "Skills: " + skills + "\n" +
               "Justification: " + justification + "\n" +
               "--------------------------------------------------";
    }
}
