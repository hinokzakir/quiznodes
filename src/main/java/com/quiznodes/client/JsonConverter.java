package com.quiznodes.client;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.quiznodes.server.types.Question;
import com.quiznodes.server.types.Quiz;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JsonConverter {
    public JsonConverter(){

    }

    public Quiz convertQuiz(File file){
        JSONObject quizJson;
        try {
            String content = Files.readString(file.toPath());
            
            quizJson = new JSONObject(content);

            String id  = UUID.randomUUID().toString();
            String name = quizJson.getString("name");

            Quiz quiz = new Quiz(id, name);

            JSONArray questions = quizJson.getJSONArray("questions");
            for(int i = 0; i < questions.length(); i++ ){
                JSONObject q = questions.getJSONObject(i);
                String questionString = q.getString("text");
                JSONArray alternatives = q.getJSONArray("alternatives");
                List<String> alternativesStrings = new ArrayList<>();
                for(int j = 0; j < alternatives.length(); j++){
                    String alternative = alternatives.getString(j);
                    alternativesStrings.add(alternative);
                }
                String correctString = q.getString("correct");

                Question question = new Question(questionString, correctString, alternativesStrings);
                quiz.addQuestion(question);
            }

            return quiz;
            
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
        }

        return null;

        
    }
}
