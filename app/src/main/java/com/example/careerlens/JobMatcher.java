
package com.example.careerlens;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JobMatcher {

    public static class MatchResult {
        public int matchScore;

        public int skillScore;
        public int educationScore;
        public int experienceScore;

        public boolean educationRequired;
        public boolean experienceRequired;

        public String educationMatch = "Not specified";
        public String experienceMatch = "Not specified";

        public List<String> matchingSkills = new ArrayList<>();
        public List<String> missingSkills = new ArrayList<>();
        public List<String> suggestions = new ArrayList<>();
    }

    public static MatchResult compare(
            String resumeText,
            String jobDescription
    ) {

        MatchResult result = new MatchResult();

        if (resumeText == null || resumeText.trim().isEmpty()) {
            result.suggestions.add("Please provide a readable resume.");
            return result;
        }

        if (jobDescription == null || jobDescription.trim().isEmpty()) {
            result.suggestions.add("Please enter a job description.");
            return result;
        }

        String resume = resumeText.toLowerCase(Locale.ROOT);
        String job = jobDescription.toLowerCase(Locale.ROOT);

        // --------------------------------
        // 1. SKILLS MATCHING
        // --------------------------------

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
        Set<String> resumeSkills = new LinkedHashSet<>();

        for (String skill : skillKeywords) {
            if (containsKeyword(job, skill)) {
                requiredSkills.add(skill);
            }

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

        if (!requiredSkills.isEmpty()) {
            result.skillScore = Math.round(
                    result.matchingSkills.size() * 100.0f
                            / requiredSkills.size()
            );
        }

        // --------------------------------
        // 2. EDUCATION MATCHING
        // --------------------------------

        String[] educationKeywords = {
                "bachelor", "b.sc", "bsc", "bca",
                "b.tech", "btech", "b.e", "be",
                "master", "m.sc", "msc", "mca",
                "m.tech", "mtech", "mba",
                "phd", "doctorate", "diploma"
        };

        Set<String> requiredEducation = new LinkedHashSet<>();

        for (String education : educationKeywords) {
            if (containsKeyword(job, education)) {
                requiredEducation.add(education);
            }
        }

        if (!requiredEducation.isEmpty()) {

            result.educationRequired = true;

            for (String education : requiredEducation) {
                if (containsKeyword(resume, education)) {
                    result.educationScore = 100;
                    result.educationMatch =
                            "Related education qualification found: " + education;
                    break;
                }
            }

            if (result.educationScore == 0) {
                result.educationMatch =
                        "Required education qualification was not detected.";

                result.suggestions.add(
                        "Add your relevant degree or educational qualification "
                                + "clearly to your resume."
                );
            }
        }

        // --------------------------------
        // 3. EXPERIENCE MATCHING
        // --------------------------------

        int requiredYears = extractYears(job);
        int resumeYears = extractYears(resume);

        if (requiredYears > 0) {

            result.experienceRequired = true;

            if (resumeYears >= requiredYears) {
                result.experienceScore = 100;
                result.experienceMatch =
                        "Detected " + resumeYears
                                + " years against " + requiredYears
                                + " required years.";
            } else {
                result.experienceScore = 0;
                result.experienceMatch =
                        "Detected " + resumeYears
                                + " years against " + requiredYears
                                + " required years.";

                result.suggestions.add(
                        "The required experience was not fully detected. "
                                + "Clearly mention your work duration, internships "
                                + "and relevant practical experience."
                );
            }

        } else {
            result.experienceMatch =
                    "No specific minimum experience requirement detected.";
        }

        // --------------------------------
        // 4. OVERALL MATCH SCORE
        // --------------------------------

        int totalScore = 0;
        int totalWeight = 0;

        if (!requiredSkills.isEmpty()) {
            totalScore += result.skillScore * 60;
            totalWeight += 60;
        }

        if (result.educationRequired) {
            totalScore += result.educationScore * 20;
            totalWeight += 20;
        }

        if (result.experienceRequired) {
            totalScore += result.experienceScore * 20;
            totalWeight += 20;
        }

        if (totalWeight > 0) {
            result.matchScore = Math.round(
                    totalScore / (float) totalWeight
            );
        }

        // --------------------------------
        // 5. SUGGESTIONS
        // --------------------------------

        if (requiredSkills.isEmpty()) {
            result.suggestions.add(
                    "No recognized skills were found in the job description. "
                            + "Try providing a more detailed job description."
            );
        } else if (!result.missingSkills.isEmpty()) {
            result.suggestions.add(
                    "Consider learning or highlighting these missing skills: "
                            + String.join(", ", result.missingSkills)
            );
        }

        if (result.skillScore < 50 && !requiredSkills.isEmpty()) {
            result.suggestions.add(
                    "Your technical skill match is low. Improve relevant "
                            + "technical skills and project experience."
            );
        } else if (result.skillScore < 80 && !requiredSkills.isEmpty()) {
            result.suggestions.add(
                    "Your technical skill match is moderate. Focus on "
                            + "strengthening missing skills."
            );
        } else if (!requiredSkills.isEmpty()) {
            result.suggestions.add(
                    "Your technical skill match is strong. Highlight "
                            + "these skills in your projects and experience."
            );
        }

        if (totalWeight == 0) {
            result.suggestions.add(
                    "The job description did not contain enough recognized "
                            + "requirements to calculate a reliable match score."
            );
        }

        return result;
    }

    // --------------------------------
    // CHECK KEYWORD
    // --------------------------------

    private static boolean containsKeyword(
            String text,
            String keyword
    ) {

        String pattern = "(?<![a-z0-9])"
                + Pattern.quote(keyword.toLowerCase(Locale.ROOT))
                + "(?![a-z0-9])";

        return Pattern.compile(pattern).matcher(text).find();
    }

    // --------------------------------
    // EXTRACT YEARS OF EXPERIENCE
    // --------------------------------

    private static int extractYears(String text) {

        Pattern pattern = Pattern.compile(
                "(\\d+)\\s*\\+?\\s*(?:years?|yrs?)"
        );

        Matcher matcher = pattern.matcher(text);

        int maximumYears = 0;

        while (matcher.find()) {
            try {
                int years = Integer.parseInt(matcher.group(1));

                if (years > maximumYears) {
                    maximumYears = years;
                }

            } catch (NumberFormatException ignored) {
                // Ignore invalid numbers
            }
        }

        return maximumYears;
    }
}
