
package com.example.careerlens;

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextView tvResumeStatus;
    private TextView tvExtractedText;

    private Uri selectedResumeUri;

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

        Button btnUploadResume =
                findViewById(R.id.btnUploadResume);

        tvResumeStatus =
                findViewById(R.id.tvResumeStatus);

        tvExtractedText =
                findViewById(R.id.tvExtractedText);

        pdfPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        selectedResumeUri = uri;

                        tvResumeStatus.setText(
                                "Resume selected successfully!"
                        );

                        extractPdfText(uri);

                    } else {
                        tvResumeStatus.setText(
                                "No resume selected"
                        );
                    }
                }
        );

        btnUploadResume.setOnClickListener(v ->
                pdfPickerLauncher.launch("application/pdf")
        );
    }

    private void extractPdfText(Uri pdfUri) {

        tvExtractedText.setText("Reading your resume...");

        executorService.execute(() -> {

            try (InputStream inputStream =
                         getContentResolver().openInputStream(pdfUri)) {

                if (inputStream == null) {
                    throw new Exception("Unable to open PDF file.");
                }

                try (PDDocument document =
                             PDDocument.load(inputStream)) {

                    PDFTextStripper textStripper =
                            new PDFTextStripper();

                    String extractedText =
                            textStripper.getText(document);

                    runOnUiThread(() -> {
                        if (extractedText.trim().isEmpty()) {
                            tvExtractedText.setText(
                                    "No readable text found in this PDF. " +
                                            "It may contain scanned images."
                            );
                        } else {
                            tvExtractedText.setText(extractedText);
                        }
                    });
                }

            } catch (Exception e) {

                runOnUiThread(() -> {
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
