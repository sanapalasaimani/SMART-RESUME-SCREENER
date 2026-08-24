import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class SmartResumeScreenerGUI extends JFrame {

    private JTextArea jobDescArea;
    private JTextField resumesDirField;
    private JTextArea outputArea;
    private JButton runButton;
    private String apiKey;

    public SmartResumeScreenerGUI() {
        setTitle("Smart Resume Screener");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Load API Key early
        Map<String, String> env = EnvParser.loadEnv(".env");
        apiKey = env.get("GROQ_API_KEY");

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        setContentPane(mainPanel);

        // Top Panel: Job Description
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(new JLabel("Job Description (Paste text here):"), BorderLayout.NORTH);
        jobDescArea = new JTextArea(8, 40);
        jobDescArea.setLineWrap(true);
        jobDescArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(jobDescArea);
        topPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        // Center Panel: Resumes & Run Button
        JPanel centerPanel = new JPanel(new BorderLayout(5, 5));
        
        JPanel filePanel = new JPanel(new BorderLayout(5, 5));
        filePanel.add(new JLabel("Resumes Folder:"), BorderLayout.WEST);
        resumesDirField = new JTextField();
        resumesDirField.setEditable(false);
        filePanel.add(resumesDirField, BorderLayout.CENTER);
        
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle("Select Resumes Directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                resumesDirField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        filePanel.add(browseButton, BorderLayout.EAST);
        
        centerPanel.add(filePanel, BorderLayout.NORTH);
        
        runButton = new JButton("Run Screener");
        runButton.setFont(new Font("Arial", Font.BOLD, 14));
        runButton.setBackground(new Color(0, 120, 215));
        runButton.setForeground(Color.WHITE);
        runButton.setFocusPainted(false);
        runButton.addActionListener(e -> runScreener());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(runButton);
        centerPanel.add(buttonPanel, BorderLayout.CENTER);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Bottom Panel: Output
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(new JLabel("Results:"), BorderLayout.NORTH);
        outputArea = new JTextArea(12, 40);
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane outputScroll = new JScrollPane(outputArea);
        bottomPanel.add(outputScroll, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        if (apiKey == null || apiKey.isEmpty()) {
            outputArea.setText("WARNING: GROQ_API_KEY not found in .env file.\nPlease add it to run the screener.");
            runButton.setEnabled(false);
        }
    }

    private void runScreener() {
        String jobDesc = jobDescArea.getText().trim();
        String resumeDir = resumesDirField.getText().trim();

        if (jobDesc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Job Description.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (resumeDir.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a Resumes folder.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        List<File> resumeFiles = ResumeParser.getResumeFiles(resumeDir);
        if (resumeFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No .txt or .pdf resumes found in the selected folder.", "Not Found", JOptionPane.WARNING_MESSAGE);
            return;
        }

        runButton.setEnabled(false);
        outputArea.setText("Found " + resumeFiles.size() + " resumes. Processing via LLM API...\nThis may take a moment per resume...\n");

        // Use SwingWorker to avoid freezing the UI
        SwingWorker<List<Candidate>, String> worker = new SwingWorker<>() {
            @Override
            protected List<Candidate> doInBackground() throws Exception {
                LLMService llmService = new LLMService(apiKey);
                List<Candidate> candidates = new ArrayList<>();

                for (File file : resumeFiles) {
                    publish("Parsing: " + file.getName() + " ...");
                    try {
                        String resumeText = ResumeParser.readTextFile(file.getAbsolutePath());
                        Candidate candidate = llmService.analyzeResume(file.getName(), resumeText, jobDesc);
                        candidates.add(candidate);
                    } catch (Exception ex) {
                        publish("Error processing " + file.getName() + ": " + ex.getMessage());
                    }
                }
                
                candidates.sort(Comparator.comparingInt(Candidate::getMatchScore).reversed());
                
                Database db = new Database("database.csv");
                db.saveCandidates(candidates);
                publish("\nSaved results to database.csv.");
                
                return candidates;
            }

            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    outputArea.append(message + "\n");
                }
            }

            @Override
            protected void done() {
                try {
                    List<Candidate> results = get();
                    outputArea.append("\n=========================================\n");
                    outputArea.append("         SHORTLISTED CANDIDATES          \n");
                    outputArea.append("=========================================\n\n");
                    
                    for (Candidate c : results) {
                        outputArea.append(c.toString() + "\n");
                    }
                } catch (Exception ex) {
                    outputArea.append("\nError during execution: " + ex.getMessage());
                } finally {
                    runButton.setEnabled(true);
                }
            }
        };

        worker.execute();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception e) {
            // ignore
        }
        SwingUtilities.invokeLater(() -> {
            new SmartResumeScreenerGUI().setVisible(true);
        });
    }
}
