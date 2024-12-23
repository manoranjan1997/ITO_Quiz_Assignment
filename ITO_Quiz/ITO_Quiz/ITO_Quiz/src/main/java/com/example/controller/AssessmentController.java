package com.example.controller;

import com.example.entity.Answer;
import com.example.entity.Candidate;
import com.example.entity.Question;
import com.example.service.AnswerService;
import com.example.service.CandidateService;
import com.example.service.QuestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/assessment")
public class AssessmentController {
    @Autowired
    private CandidateService candidateService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private AnswerService answerService;

    @GetMapping("/questions/{candidateId}")
    public ResponseEntity<?> getQuestions(@PathVariable int candidateId) {
        Candidate candidate = candidateService.getCandidateById(candidateId);
        if (candidate == null) {
            return ResponseEntity.badRequest().body("Candidate ID doesn’t exist.");
        }

        if (candidate.isStarted()) {
            return ResponseEntity.ok("Exam Assessment Running.");
        }

        candidate.setStarted(true);
        candidateService.updateCandidate(candidateId, candidate);

        List<Question> allQuestions = questionService.getAllQuestions();
        Collections.shuffle(allQuestions);
        List<Question> selectedQuestions = allQuestions.stream()
                .limit(10)
                .collect(Collectors.toList());
        List<Map<String, Object>> questionSet = selectedQuestions.stream()
                .map(q -> Map.of(
                        "questionId", q.getQuestionId(),
                        "question", q.getQuestion(),
                        "options", List.of(q.getOption1(), q.getOption2(), q.getOption3(), q.getOption4())))
                .collect(Collectors.toList());
        return ResponseEntity.ok(questionSet);
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submitAnswerSheet(@RequestParam int candidateId, @RequestBody List<Answer> answers) {
        Candidate candidate = candidateService.getCandidateById(candidateId);
        if (candidate == null) {
            return ResponseEntity.badRequest().body("Candidate ID doesn’t exist.");
        }

        if (candidate.isSubmit()) {
            return ResponseEntity.badRequest().body("Answer already submitted.");
        }

        try {
            for (Answer answer : answers) {
                answer.setCandidateId(candidateId);
                answerService.createAnswer(answer);
            }
            candidate.setSubmit(true);
            candidateService.updateCandidate(candidateId, candidate);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to submit answers.");
        }

        long correctAnswers = answers.stream()
                .filter(answer -> {
                    Question question = questionService.getQuestionById(answer.getQuestionId());
                    return question != null && question.getAnswer().equals(answer.getAnswer());
                }).count();

        String resultMessage = correctAnswers > 6
                ? "Candidate ID: " + candidateId + " is selected for the next round.\nCorrect Answers: " + correctAnswers + "\nIncorrect Answers: " + (answers.size() - correctAnswers)
                : "Sorry, you are not selected. Better luck next time.\nCorrect Answers: " + correctAnswers + "\nIncorrect Answers: " + (answers.size() - correctAnswers);

        return ResponseEntity.ok(resultMessage);
    }
}
