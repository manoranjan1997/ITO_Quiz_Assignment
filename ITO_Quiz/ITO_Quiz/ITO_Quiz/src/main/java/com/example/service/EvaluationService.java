package com.example.service;

import com.example.entity.Answer;
import com.example.entity.Candidate;
import com.example.repository.AnswerRepository;
import com.example.repository.CandidateRepository;
import com.example.repository.QuestionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class EvaluationService {
    @Autowired
    private CandidateRepository candidateRepository;
    @Autowired
    private AnswerRepository answerRepository;
    @Autowired
    private QuestionRepository questionRepository;

    public String evaluateCandidate(int candidateId) {
        Candidate candidate = candidateRepository.findById(candidateId).orElse(null);
        if (candidate == null) {
            return "Candidate ID doesn’t exist.";
        }

        List<Answer> answers = answerRepository.findByCandidateId(candidateId);
        long correctCount = answers.stream()
                .filter(answer -> questionRepository.findById(answer.getQuestionId())
                        .map(question -> question.getAnswer().equals(answer.getAnswer()))
                        .orElse(false))
                .count();

        int totalQuestions = answers.size();
        int incorrectCount = (int)totalQuestions - (int) correctCount;
        String resultMessage = correctCount > 6
                ? candidateId + " : " + candidate.getName() + " is selected for the next round."
                : candidateId + " : " + candidate.getName() + " is rejected in this round.";

        resultMessage += "\nCorrect Answers: " + correctCount;
        resultMessage += "\nIncorrect Answers: " + incorrectCount;

        return resultMessage;
    }
}
