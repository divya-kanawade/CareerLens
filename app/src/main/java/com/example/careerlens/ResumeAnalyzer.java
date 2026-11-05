
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
        public List<String> missingSections = new ArrayList<>();

        public int qualityScore = 0;
        public int completedSections = 0;
        public int totalSections = 7;
    }

    public static AnalysisResult analyze(String resumeText) {

        AnalysisResult result = new AnalysisResult();

        if (resumeText == null || resumeText.trim().isEmpty()) {
            result.suggestions.add("No resume text was provided. Upload a valid text-based PDF.");
            result.missingSections.add("Resume content");
            return result;
        }

        String text = resumeText.toLowerCase(Locale.ROOT);

        // 1. Extract technical skills
        String[] skillKeywords = {
                "java", "python", "javascript", "c++", "c#",
                "react", "angular", "node.js", "html", "css",
                "sql", "mysql", "mongodb", "django", "spring boot",
                "aws", "azure", "git", "github", "machine learning",
                "data analysis", "fastapi", "express.js",
                "docker", "kubernetes", "php", "typescript",
                "next.js", "rest api", "firebase", "postgresql"
        };

        Set<String> foundSkills = new LinkedHashSet<>();

        for (String skill : skillKeywords) {
            if (containsKeyword(text, skill)) {
                foundSkills.add(skill);
            }
        }

        result.skills.addAll(foundSkills);

        // 2. Extract education details
        String[] educationKeywords = {
                "bachelor", "master", "b.tech", "m.tech",
                "b.sc", "m.sc", "bca", "mca", "university",
                "college", "degree", "education", "diploma",
                "cgpa", "computer science", "information technology"
        };

        extractMatchingLines(resumeText, educationKeywords, result.education);

        // 3. Extract experience details
        String[] experienceKeywords = {
                "intern", "internship", "experience",
                "employment", "worked at", "work experience",
                "professional experience", "developer",
                "responsibilities"
        };

        extractMatchingLines(resumeText, experienceKeywords, result.experience);

        // 4. Extract project details
        String[] projectKeywords = {
                "project", "developed", "built",
                "implemented", "application",
                "system", "website"
        };

        extractMatchingLines(resumeText, projectKeywords, result.projects);

        // 5. Check resume sections
        boolean hasContact = containsAny(text,
                "email", "phone", "mobile", "contact", "@");

        boolean hasSummary = containsAny(text,
                "summary", "objective", "profile",
                "about me", "career objective");

        boolean hasEducation = !result.education.isEmpty()
                || containsAny(text, "education", "academic qualification");

        boolean hasSkills = !result.skills.isEmpty()
                || containsAny(text, "technical skills", "skills");

        boolean hasExperience = !result.experience.isEmpty()
                || containsAny(text, "experience", "internship", "employment");

        boolean hasProjects = !result.projects.isEmpty()
                || containsAny(text, "projects", "personal projects");

        boolean hasAchievements = containsAny(text,
                "certification", "certifications", "achievement",
                "achievements", "awards", "courses");

        // 6. Calculate resume completeness score
        int score = 0;

        if (hasContact) {
            score += 15;
            result.completedSections++;
        } else {
            result.missingSections.add("Contact Information");
            result.suggestions.add(
                    "Add your professional email address and phone number."
            );
        }

        if (hasSummary) {
            score += 10;
            result.completedSections++;
        } else {
            result.missingSections.add("Professional Summary");
            result.suggestions.add(
                    "Add a short professional summary describing your background and career goals."
            );
        }

        if (hasEducation) {
            score += 15;
            result.completedSections++;
        } else {
            result.missingSections.add("Education");
            result.suggestions.add(
                    "Include your degree, college or university, and graduation details."
            );
        }

        if (hasSkills) {
            score += 15;
            result.completedSections++;
        } else {
            result.missingSections.add("Technical Skills");
            result.suggestions.add(
                    "Add a dedicated skills section with relevant technical skills."
            );
        }

        if (hasExperience) {
            score += 15;
            result.completedSections++;
        } else {
            result.missingSections.add("Experience");
            result.suggestions.add(
                    "Include internships, work experience, volunteering, or practical experience."
            );
        }

        if (hasProjects) {
            score += 15;
            result.completedSections++;
        } else {
            result.missingSections.add("Projects");
            result.suggestions.add(
                    "Add academic or personal projects and explain your contributions."
            );
        }

        if (hasAchievements) {
            score += 15;
            result.completedSections++;
        } else {
            result.missingSections.add("Certifications and Achievements");
            result.suggestions.add(
                    "Include relevant certifications, courses, awards, or achievements if available."
            );
        }

        result.qualityScore = score;

        // 7. Additional improvement suggestions
        if (result.skills.size() < 5) {
            result.suggestions.add(
                    "Add more relevant skills that accurately represent your abilities."
            );
        }

        if (result.projects.size() > 0 &&
                !containsAny(text, "github", "portfolio")) {
            result.suggestions.add(
                    "Consider adding links to your GitHub profile or project portfolio."
            );
        }

        if (containsAny(text, "responsibilities") &&
                !containsAny(text, "achieved", "improved", "increased", "reduced")) {
            result.suggestions.add(
                    "Highlight measurable results and achievements instead of only listing responsibilities."
            );
        }

        if (result.suggestions.isEmpty()) {
            result.suggestions.add(
                    "Your resume contains all the sections checked. Review the content for accuracy and relevance."
            );
        }

        return result;
    }

    private static boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (containsKeyword(text, keyword)) {
                return true;
            }
        }
        return false;
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
