
package com.example.careerlens;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView tvResumeStatus;
    private TextView tvExtractedText;
    private TextView tvHistory;
    private EditText etJobDescription;

    private Uri selectedResumeUri;
    private String extractedResumeText = "";

    private DatabaseHelper databaseHelper;

    private ActivityResultLauncher<String> pdfPickerLauncher;

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.enableEdgeToEdge(getWindow());
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars());

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                });

        PDFBoxResourceLoader.init(getApplicationContext());

        databaseHelper = new DatabaseHelper(this);

        Button btnUploadResume = findViewById(R.id.btnUploadResume);
        Button btnAnalyze = findViewById(R.id.btnAnalyze);

        tvResumeStatus = findViewById(R.id.tvResumeStatus);
        tvExtractedText = findViewById(R.id.tvExtractedText);
        tvHistory = findViewById(R.id.tvHistory);

        etJobDescription = findViewById(R.id.etJobDescription);

        // Display previously saved analyses
        loadHistory();

        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedResumeUri = uri;
                        extractedResumeText = "";

                        tvResumeStatus.setText(
                                "Resume selected successfully!");

                        tvExtractedText.setText(
                                "Reading your resume...");

                        extractPdfText(uri);

                    } else {
                        tvResumeStatus.setText("No resume selected");
                    }
                }
        );

        btnUploadResume.setOnClickListener(v ->
                pdfPickerLauncher.launch("application/pdf")
        );

        btnAnalyze.setOnClickListener(v -> {

            if (extractedResumeText.trim().isEmpty()) {
                Toast.makeText(
                        MainActivity.this,
                        "Please select a readable resume PDF first.",
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
    }

    private void extractPdfText(Uri pdfUri) {

        executorService.execute(() -> {

            try (InputStream inputStream =
                         getContentResolver().openInputStream(pdfUri)) {

                if (inputStream == null) {
                    throw new Exception("Unable to open PDF file.");
                }

                try (PDDocument document = PDDocument.load(inputStream)) {

                    PDFTextStripper textStripper = new PDFTextStripper();

                    String extractedText =
                            textStripper.getText(document);

                    runOnUiThread(() -> {
                        if (extractedText.trim().isEmpty()) {
                            extractedResumeText = "";

                            tvExtractedText.setText(
                                    "No readable text found in this PDF. " +
                                            "It may contain scanned images."
                            );
                        } else {
                            extractedResumeText = extractedText;

                            tvExtractedText.setText(extractedText);

                            Toast.makeText(
                                    MainActivity.this,
                                    "Resume text extracted successfully!",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    });
                }

            } catch (Exception e) {

                runOnUiThread(() -> {
                    extractedResumeText = "";

                    tvExtractedText.setText(
                            "Could not read PDF: " + e.getMessage()
                    );

                    Toast.makeText(
                            MainActivity.this,
                            "PDF extraction failed",
                            Toast.LENGTH_LONG
                    ).show();
                });
            }
        });
    }

    private void analyzeResume() {

        ResumeAnalyzer.AnalysisResult result =
                ResumeAnalyzer.analyze(extractedResumeText);

        StringBuilder report = new StringBuilder();

        report.append("========== RESUME ANALYSIS ==========\n\n");

        report.append("1. TECHNICAL SKILLS\n\n");
        appendList(report, result.skills);

        report.append("\n2. EDUCATION DETAILS\n\n");
        appendList(report, result.education);

        report.append("\n3. WORK EXPERIENCE\n\n");
        appendList(report, result.experience);

        report.append("\n4. PROJECTS\n\n");
        appendList(report, result.projects);

        report.append("\n5. IMPROVEMENT SUGGESTIONS\n\n");
        appendList(report, result.suggestions);

        report.append("\n=====================================");

        tvExtractedText.setText(report.toString());

        // Save general resume analysis in SQLite
        String resumeName = getResumeName();

        long savedId = databaseHelper.saveAnalysis(
                resumeName,
                0,
                joinList(result.skills),
                "Not applicable",
                joinList(result.suggestions)
        );

        if (savedId != -1) {
            Toast.makeText(
                    this,
                    "Resume analysis saved successfully!",
                    Toast.LENGTH_SHORT
            ).show();
        }

        loadHistory();
    }

    private void analyzeJobMatch(String jobDescription) {

        JobMatcher.MatchResult result =
                JobMatcher.compare(
                        extractedResumeText,
                        jobDescription
                );

        StringBuilder report = new StringBuilder();

        report.append("========== JOB MATCH ANALYSIS ==========\n\n");

        report.append("RESUME MATCH SCORE\n");
        report.append(result.matchScore).append("%\n\n");

        report.append("MATCHING SKILLS\n\n");
        appendList(report, result.matchingSkills);

        report.append("\nMISSING SKILLS\n\n");
        appendList(report, result.missingSkills);

        report.append("\nIMPROVEMENT SUGGESTIONS\n\n");
        appendList(report, result.suggestions);

        report.append("\n========================================");

        tvExtractedText.setText(report.toString());

        // Save job match analysis in SQLite
        String resumeName = getResumeName();

        long savedId = databaseHelper.saveAnalysis(
                resumeName,
                result.matchScore,
                joinList(result.matchingSkills),
                joinList(result.missingSkills),
                joinList(result.suggestions)
        );

        if (savedId != -1) {
            Toast.makeText(
                    this,
                    "Job match analysis saved successfully!",
                    Toast.LENGTH_SHORT
            ).show();
        }

        loadHistory();
    }

    private String getResumeName() {
        if (selectedResumeUri != null &&
                selectedResumeUri.getLastPathSegment() != null) {
            return selectedResumeUri.getLastPathSegment();
        }

        return "Selected Resume";
    }

    private String joinList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "None";
        }

        return android.text.TextUtils.join(", ", items);
    }

    private void loadHistory() {

        List<String> history = databaseHelper.getAllAnalyses();

        if (history.isEmpty()) {
            tvHistory.setText("No previous analyses yet.");
            return;
        }

        StringBuilder historyText = new StringBuilder();

        historyText.append("========== SAVED ANALYSES ==========\n\n");

        for (String record : history) {
            historyText.append(record).append("\n\n");
        }

        tvHistory.setText(historyText.toString());
    }

    private void appendList(
            StringBuilder report,
            List<String> items) {

        if (items == null || items.isEmpty()) {
            report.append("No information detected.\n");
            return;
        }

        for (String item : items) {
            report.append("• ").append(item).append("\n");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
