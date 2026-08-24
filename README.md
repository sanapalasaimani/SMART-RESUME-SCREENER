# Smart Resume Screener

![Demo Video](demo_video.webp)

A native Java CLI application for screening resumes against a job description using a Large Language Model (LLM).

## Features
- Intelligently parses text resumes and job descriptions.
- Extracts structured data: skills, experience, and education.
- Computes a match score (1-10) with justification using the Groq API (llama-3.1-8b-instant).
- Saves results persistently to a local CSV database (`database.csv`).
- Displays a shortlisted ranking in the command prompt.

## Prerequisites
- Java 17 or higher
- A Groq API Key

## Setup
1. Create a `.env` file in the root directory (same level as `src`).
2. Add your Groq API key to the `.env` file:
   ```
   GROQ_API_KEY=your_api_key_here
   ```

## Compilation
Open a command prompt in the root directory and run:
```bash
javac -cp "src;lib/*" src/*.java
```

## Execution
Run the compiled application to open the graphical interface:
```bash
java -cp "src;lib/*" SmartResumeScreenerGUI
```

### Usage Instructions
1. Paste your Job Description text directly into the designated area.
2. Click "Browse..." and select the directory containing the resume text files (e.g., `resumes/`).
3. Click "Run Screener". The application will process each resume, communicate with the LLM, and display the shortlisted candidates visually in the text box.
4. The results are also saved locally to `database.csv`.

## LLM Prompts
The core prompt used for candidate evaluation is:
> "Compare the following resume with this job description. Extract structured data: skills, experience, education. Compute a match score between candidate and job description (1-10). Provide a justification. Return exactly a JSON object (and nothing else) with keys: 'skills', 'experience', 'education', 'matchScore' (integer), and 'justification'."
