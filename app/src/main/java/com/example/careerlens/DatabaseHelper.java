
package com.example.careerlens;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "careerlens.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_ANALYSIS = "analysis_history";

    private static final String COL_ID = "id";
    private static final String COL_RESUME_NAME = "resume_name";
    private static final String COL_MATCH_SCORE = "match_score";
    private static final String COL_MATCHING_SKILLS = "matching_skills";
    private static final String COL_MISSING_SKILLS = "missing_skills";
    private static final String COL_SUGGESTIONS = "suggestions";
    private static final String COL_ANALYSIS_DATE = "analysis_date";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String createTable = "CREATE TABLE " + TABLE_ANALYSIS + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_RESUME_NAME + " TEXT, " +
                COL_MATCH_SCORE + " INTEGER, " +
                COL_MATCHING_SKILLS + " TEXT, " +
                COL_MISSING_SKILLS + " TEXT, " +
                COL_SUGGESTIONS + " TEXT, " +
                COL_ANALYSIS_DATE + " TEXT" +
                ")";

        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(
            SQLiteDatabase db,
            int oldVersion,
            int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ANALYSIS);
        onCreate(db);
    }

    public long saveAnalysis(
            String resumeName,
            int matchScore,
            String matchingSkills,
            String missingSkills,
            String suggestions) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(COL_RESUME_NAME, resumeName);
        values.put(COL_MATCH_SCORE, matchScore);
        values.put(COL_MATCHING_SKILLS, matchingSkills);
        values.put(COL_MISSING_SKILLS, missingSkills);
        values.put(COL_SUGGESTIONS, suggestions);

        String date = new SimpleDateFormat(
                "dd MMM yyyy, hh:mm a",
                Locale.getDefault()
        ).format(new Date());

        values.put(COL_ANALYSIS_DATE, date);

        return db.insert(TABLE_ANALYSIS, null, values);
    }

    public List<String> getAllAnalyses() {

        List<String> history = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_ANALYSIS,
                null,
                null,
                null,
                null,
                null,
                COL_ID + " DESC"
        );

        try {
            while (cursor.moveToNext()) {

                int id = cursor.getInt(
                        cursor.getColumnIndexOrThrow(COL_ID));

                String resumeName = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_RESUME_NAME));

                int score = cursor.getInt(
                        cursor.getColumnIndexOrThrow(COL_MATCH_SCORE));

                String matchingSkills = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_MATCHING_SKILLS));

                String missingSkills = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_MISSING_SKILLS));

                String suggestions = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_SUGGESTIONS));

                String date = cursor.getString(
                        cursor.getColumnIndexOrThrow(COL_ANALYSIS_DATE));

                String record =
                        "Analysis ID: " + id +
                                "\nResume: " + resumeName +
                                "\nMatch Score: " + score + "%" +
                                "\nDate: " + date +
                                "\nMatching Skills: " + matchingSkills +
                                "\nMissing Skills: " + missingSkills +
                                "\nSuggestions: " + suggestions +
                                "\n------------------------------";

                history.add(record);
            }
        } finally {
            cursor.close();
        }

        return history;
    }

    public boolean deleteAnalysis(int id) {

        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(
                TABLE_ANALYSIS,
                COL_ID + " = ?",
                new String[]{String.valueOf(id)}
        );

        return rowsDeleted > 0;
    }
}
