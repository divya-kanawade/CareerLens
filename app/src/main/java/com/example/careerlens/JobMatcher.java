
package com.example.careerlens;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class JobMatcher {

    public static class MatchResult {
        public int matchScore;
        public List<String> matchingSkills = new ArrayList<>();
        public List<String> missingSkills = new ArrayList<>();
        public List<String> suggestions = new ArrayList<>();
    }

    public static MatchResult compare(String resumeText, String jobDescription) {

        MatchResult result = new MatchResult();

        if (resumeText == null || resumeText.trim().isEmpty()) {
            result.suggestions.add("Please provide a readable resume.");
            return result;
        }

        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            result.suggestions.add("Please enter a job description to compare.");
            return result;
        }

        String resume = resumeText.toLowerCase(Locale.ROOT);
        String job = jobDescription.toLowerCase(Locale.ROOT);

        String[] skillKeywords = {
                "java", "python", "javascript", "c++", "c#",
                "react", "angular", "node.js", "html", "css",
                "sql", "mysql", "mongodb", "django", "spring boot",
                "aws", "azure", "git", "github", "machine learning",
                "data analysis", "fastapi", "express.js",
                "docker", "kubernetes", "rest api", "typescript",
                "communication", "problem solving", "teamwork"
        };

        Set<String> requiredSkills = new LinkedHashSet<>();

        for (String skill : skillKeywords) {
            if (containsKeyword(job, skill)) {
                requiredSkills.add(skill);
            }
        }

        Set<String> resumeSkills = new LinkedHashSet<>();

        for (String skill : skillKeywords) {
            if (containsKeyword(resume, skill)) {
                resumeSkills.add(skill);
            }
        }

        for (String skill : requiredSkills) {
            if (resumeSkills.contains(skill)) {
                result.matchingSkills.add(skill);
            } else {
                result.missingSkills.add(skill);
            }
        }

        if (requiredSkills.isEmpty()) {
            result.suggestions.add(
                    "No recognized skills were found in the job description. " +
                            "Try providing a more detailed job description."
            );
            result.matchScore = 0;
            return result;
        }

        result.matchScore = Math.round(
                (result.matchingSkills.size() * 100.0f)
                        / requiredSkills.size()
        );

        if (!result.missingSkills.isEmpty()) {
            result.suggestions.add(
                    "Consider learning or highlighting these missing skills: "
                            + String.join(", ", result.missingSkills)
            );
        }

        if (result.matchScore < 50) {
            result.suggestions.add(
                    "Your resume has a low skill match. Consider improving " +
                            "your relevant technical skills and project experience."
            );
        } else if (result.matchScore < 80) {
            result.suggestions.add(
                    "Your resume has a moderate skill match. Focus on " +
                            "strengthening the missing skills."
            );
        } else {
            result.suggestions.add(
                    "Your resume has a strong skill match. " +
                            "Make sure your experience and projects clearly " +
                            "demonstrate these skills."
            );
        }

        return result;
    }

    private static boolean containsKeyword(String text, String keyword) {

        String pattern = "(?<![a-z0-9])"
                + Pattern.quote(keyword.toLowerCase(Locale.ROOT))
                + "(?![a-z0-9])";

        return Pattern.compile(pattern).matcher(text).find();
    }
}
