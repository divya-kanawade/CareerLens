
package com.example.careerlens;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class ResumeAnalyzer {

    public static class AnalysisResult {
        public List<String> skills = new ArrayList<>();
        public List<String> education = new ArrayList<>();
        public List<String> experience = new ArrayList<>();
        public List<String> projects = new ArrayList<>();
        public List<String> suggestions = new ArrayList<>();
    }

    public static AnalysisResult analyze(String resumeText) {
        AnalysisResult result = new AnalysisResult();

        if (resumeText == null || resumeText.trim().isEmpty()) {
            result.suggestions.add("No resume text was provided.");
            return result;
        }

        String text = resumeText.toLowerCase(Locale.ROOT);

        String[] skillKeywords = {
                "java", "python", "javascript", "c++", "c#", "react",
                "angular", "node.js", "html", "css", "sql", "mysql",
                "mongodb", "django", "spring boot", "aws", "azure",
                "git", "github", "machine learning", "data analysis",
                "fastapi", "express.js"
        };

        Set<String> foundSkills = new LinkedHashSet<>();

        for (String skill : skillKeywords) {
            if (containsKeyword(text, skill)) {
                foundSkills.add(skill);
            }
        }

        result.skills.addAll(foundSkills);

        String[] educationKeywords = {
                "bachelor", "master", "b.tech", "m.tech", "b.sc",
                "m.sc", "bca", "mca", "university", "college",
                "degree", "education", "diploma", "cgpa"
        };

        extractMatchingLines(resumeText, educationKeywords, result.education);

        String[] experienceKeywords = {
                "intern", "internship", "experience", "employment",
                "worked at", "work experience", "professional experience"
        };

        extractMatchingLines(resumeText, experienceKeywords, result.experience);

        String[] projectKeywords = {
                "project", "developed", "built", "implemented", "application"
        };

        extractMatchingLines(resumeText, projectKeywords, result.projects);

        if (result.skills.isEmpty()) {
            result.suggestions.add("Add a dedicated technical skills section.");
        }

        if (result.education.isEmpty()) {
            result.suggestions.add("Add your education details.");
        }

        if (result.experience.isEmpty()) {
            result.suggestions.add("Include internships, work experience, or relevant practical experience.");
        }

        if (result.projects.isEmpty()) {
            result.suggestions.add("Add projects with descriptions of your contributions.");
        }

        if (result.skills.size() < 5) {
            result.suggestions.add("Consider including more relevant technical skills.");
        }

        if (result.suggestions.isEmpty()) {
            result.suggestions.add("Your resume contains the main sections checked by this basic analyzer. Review the details for accuracy.");
        }

        return result;
    }

    private static boolean containsKeyword(String text, String keyword) {
        String pattern = "(?<![a-z0-9])"
                + Pattern.quote(keyword.toLowerCase(Locale.ROOT))
                + "(?![a-z0-9])";

        return Pattern.compile(pattern).matcher(text).find();
    }

    private static void extractMatchingLines(
            String resumeText,
            String[] keywords,
            List<String> output) {

        Set<String> uniqueLines = new LinkedHashSet<>();
        String[] lines = resumeText.split("\\r?\\n");

        for (String line : lines) {
            String cleanedLine = line.trim();

            if (cleanedLine.isEmpty()) {
                continue;
            }

            String lowerLine = cleanedLine.toLowerCase(Locale.ROOT);

            for (String keyword : keywords) {
                if (containsKeyword(lowerLine, keyword)) {
                    uniqueLines.add(cleanedLine);
                    break;
                }
            }
        }

        output.addAll(uniqueLines);
    }
}
