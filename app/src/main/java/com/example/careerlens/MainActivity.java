
package com.example.careerlens;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // Android emulator connects to the Mac backend through 10.0.2.2
    private static final String API_URL =
            "http://127.0.0.1:8000/analyses";

    private Button btnUploadResume, btnAnalyze;
    private TextView tvResumeStatus, tvExtractedText, tvHistory;
    private EditText etJobDescription;

    private Uri selectedResumeUri;
    private String extractedResumeText = "";
    private String selectedResumeName = "Resume.pdf";

    private DatabaseHelper databaseHelper;
    private ExecutorService executorService;

    private ActivityResultLauncher<String> pdfPickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PDFBoxResourceLoader.init(getApplicationContext());

        executorService = Executors.newSingleThreadExecutor();
        databaseHelper = new DatabaseHelper(this);

        btnUploadResume = findViewById(R.id.btnUploadResume);
        btnAnalyze = findViewById(R.id.btnAnalyze);
        tvResumeStatus = findViewById(R.id.tvResumeStatus);
        tvExtractedText = findViewById(R.id.tvExtractedText);
        tvHistory = findViewById(R.id.tvHistory);
        etJobDescription = findViewById(R.id.etJobDescription);

        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedResumeUri = uri;
                        selectedResumeName = getFileName(uri);

                        tvResumeStatus.setText(
                                "Selected: " + selectedResumeName
                        );

                        extractPdfText(uri);
                    }
                }
        );

        btnUploadResume.setOnClickListener(v ->
                pdfPickerLauncher.launch("application/pdf")
        );

        btnAnalyze.setOnClickListener(v -> {

            if (extractedResumeText.trim().isEmpty()) {
                Toast.makeText(
                        this,
                        "Please select a resume first",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            String jobDescription =
                    etJobDescription.getText().toString().trim();

            if (jobDescription.isEmpty()) {
                analyzeResume();
            } else {
                analyzeJobMatch(jobDescription);
            }
        });

        loadHistory();
    }

    // Get selected PDF filename
    private String getFileName(Uri uri) {

        String fileName = "Resume.pdf";

        Cursor cursor = getContentResolver().query(
                uri, null, null, null, null
        );

        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(
                            OpenableColumns.DISPLAY_NAME
                    );

                    if (index != -1) {
                        fileName = cursor.getString(index);
                    }
                }
            } finally {
                cursor.close();
            }
        }

        return fileName;
    }

    // Extract text from PDF
    private void extractPdfText(Uri uri) {

        tvExtractedText.setText("Extracting resume text...");

        executorService.execute(() -> {

            String extractedText = "";

            try (InputStream inputStream =
                         getContentResolver().openInputStream(uri)) {

                if (inputStream == null) {
                    throw new Exception("Unable to open PDF");
                }

                try (PDDocument document =
                             PDDocument.load(inputStream)) {

                    PDFTextStripper stripper = new PDFTextStripper();
                    extractedText = stripper.getText(document);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

            String finalText = extractedText;

            runOnUiThread(() -> {

                if (finalText.trim().isEmpty()) {

                    extractedResumeText = "";

                    tvExtractedText.setText(
                            "No readable text found. Please select a text-based PDF."
                    );

                    Toast.makeText(
                            MainActivity.this,
                            "Could not extract text from PDF",
                            Toast.LENGTH_LONG
                    ).show();

                } else {

                    extractedResumeText = finalText;

                    tvExtractedText.setText(
                            "Resume text extracted successfully.\n\n"
                                    + finalText
                    );

                    Toast.makeText(
                            MainActivity.this,
                            "Resume uploaded successfully",
                            Toast.LENGTH_SHORT
                    ).show();
                }
            });
        });
    }

    // General resume analysis
    private void analyzeResume() {

        ResumeAnalyzer.AnalysisResult result =
                ResumeAnalyzer.analyze(extractedResumeText);

        String skills = listToString(result.skills);
        String education = listToString(result.education);
        String experience = listToString(result.experience);
        String projects = listToString(result.projects);
        String missingSections = listToString(result.missingSections);
        String suggestions = listToString(result.suggestions);

        String output =
                "RESUME ANALYSIS\n\n"
                        + "RESUME QUALITY SCORE\n"
                        + result.qualityScore + "/100\n\n"
                        + "SECTION COMPLETENESS\n"
                        + result.completedSections + " out of "
                        + result.totalSections + " sections detected\n\n"
                        + "MISSING SECTIONS\n"
                        + missingSections + "\n"
                        + "SKILLS\n"
                        + skills + "\n"
                        + "EDUCATION\n"
                        + education + "\n"
                        + "EXPERIENCE\n"
                        + experience + "\n"
                        + "PROJECTS\n"
                        + projects + "\n"
                        + "IMPROVEMENT SUGGESTIONS\n"
                        + suggestions;

        tvExtractedText.setText(output);

        // Save analysis in SQLite
        databaseHelper.saveAnalysis(
                selectedResumeName,
                0,
                skills,
                missingSections,
                suggestions
        );

        loadHistory();

        // Save analysis in MongoDB through FastAPI
        sendAnalysisToMongoDB(
                "resume",
                null,
                result.qualityScore,
                result.skills,
                null,
                result.suggestions,
                null,
                null
        );

        Toast.makeText(
                this,
                "Resume analysis completed. Saving to database...",
                Toast.LENGTH_SHORT
        ).show();
    }

    // Job description matching
    private void analyzeJobMatch(String jobDescription) {

        JobMatcher.MatchResult result =
                JobMatcher.compare(
                        extractedResumeText,
                        jobDescription
                );

        String matchingSkills =
                listToString(result.matchingSkills);

        String missingSkills =
                listToString(result.missingSkills);

        String suggestions =
                listToString(result.suggestions);

        String educationResult;

        if (result.educationRequired) {
            educationResult =
                    result.educationMatch
                            + "\nEducation Score: "
                            + result.educationScore + "%";
        } else {
            educationResult =
                    "No specific education requirement detected in the job description.";
        }

        String experienceResult;

        if (result.experienceRequired) {
            experienceResult =
                    result.experienceMatch
                            + "\nExperience Score: "
                            + result.experienceScore + "%";
        } else {
            experienceResult = result.experienceMatch;
        }

        String output =
                "JOB DESCRIPTION MATCH RESULT\n\n"
                        + "OVERALL MATCH SCORE\n"
                        + result.matchScore + "%\n\n"
                        + "SKILLS MATCH\n"
                        + "Skill Score: " + result.skillScore + "%\n\n"
                        + "Matching Skills:\n"
                        + matchingSkills + "\n"
                        + "Missing Skills:\n"
                        + missingSkills + "\n\n"
                        + "EDUCATION MATCH\n"
                        + educationResult + "\n\n"
                        + "EXPERIENCE MATCH\n"
                        + experienceResult + "\n\n"
                        + "SUGGESTIONS\n"
                        + suggestions;

        tvExtractedText.setText(output);

        // Save analysis in SQLite
        databaseHelper.saveAnalysis(
                selectedResumeName,
                result.matchScore,
                matchingSkills,
                missingSkills,
                suggestions
        );

        loadHistory();

        // Save job matching result in MongoDB
        sendAnalysisToMongoDB(
                "job_match",
                result.matchScore,
                null,
                result.matchingSkills,
                result.missingSkills,
                result.suggestions,
                educationResult,
                experienceResult
        );

        Toast.makeText(
                this,
                "Job matching completed. Saving to database...",
                Toast.LENGTH_SHORT
        ).show();
    }

    // Send analysis to MongoDB through FastAPI
    private void sendAnalysisToMongoDB(
            String analysisType,
            Integer matchScore,
            Integer qualityScore,
            List<String> skills,
            List<String> missingSkills,
            List<String> suggestions,
            String educationMatch,
            String experienceMatch
    ) {

        executorService.execute(() -> {

            HttpURLConnection connection = null;

            try {
                URL url = new URL(API_URL);

                connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

                connection.setRequestProperty(
                        "Accept",
                        "application/json"
                );

                // Create JSON request
                JSONObject json = new JSONObject();

                json.put("resumeName", selectedResumeName);
                json.put("analysisType", analysisType);

                if (matchScore != null) {
                    json.put("matchScore", matchScore);
                }

                if (qualityScore != null) {
                    json.put("qualityScore", qualityScore);
                }

                json.put("skills", new JSONArray(skills));
                json.put(
                        "missingSkills",
                        new JSONArray(
                                missingSkills == null
                                        ? java.util.Collections.emptyList()
                                        : missingSkills
                        )
                );

                json.put(
                        "suggestions",
                        new JSONArray(
                                suggestions == null
                                        ? java.util.Collections.emptyList()
                                        : suggestions
                        )
                );

                if (educationMatch != null) {
                    json.put("educationMatch", educationMatch);
                }

                if (experienceMatch != null) {
                    json.put("experienceMatch", experienceMatch);
                }

                // Send request to FastAPI
                byte[] requestBody = json.toString()
                        .getBytes(StandardCharsets.UTF_8);

                try (OutputStream outputStream =
                             connection.getOutputStream()) {

                    outputStream.write(requestBody);
                    outputStream.flush();
                }

                int responseCode = connection.getResponseCode();

                if (responseCode >= 200 && responseCode < 300) {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "Analysis successfully saved to MongoDB",
                                    Toast.LENGTH_LONG
                            ).show()
                    );

                } else {

                    runOnUiThread(() ->
                            Toast.makeText(
                                    MainActivity.this,
                                    "MongoDB save failed. HTTP "
                                            + responseCode,
                                    Toast.LENGTH_LONG
                            ).show()
                    );
                }

            } catch (Exception e) {

                e.printStackTrace();

                String errorMessage = e.getClass().getSimpleName()
                        + ": " + e.getMessage();

                runOnUiThread(() ->
                        Toast.makeText(
                                MainActivity.this,
                                "Backend connection failed: "
                                        + errorMessage,
                                Toast.LENGTH_LONG
                        ).show()
                );

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    // Convert list into readable text
    private String listToString(List<String> items) {

        if (items == null || items.isEmpty()) {
            return "Not detected";
        }

        StringBuilder builder = new StringBuilder();

        for (String item : items) {
            builder.append("• ")
                    .append(item)
                    .append("\n");
        }

        return builder.toString();
    }

    // Load saved analysis history from SQLite
    private void loadHistory() {

        List<String> history = databaseHelper.getAllAnalyses();

        StringBuilder builder = new StringBuilder();

        builder.append("PREVIOUS ANALYSES\n\n");

        if (history == null || history.isEmpty()) {
            builder.append("No previous analyses found.");
        } else {
            for (String item : history) {
                builder.append(item).append("\n\n");
            }
        }

        tvHistory.setText(builder.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (executorService != null) {
            executorService.shutdown();
        }

        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}

