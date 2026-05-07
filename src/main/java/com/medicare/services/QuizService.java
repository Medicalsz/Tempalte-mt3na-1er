package com.medicare.services;

import com.medicare.models.Quiz;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class QuizService {

    private static final List<Quiz> quizzes = Arrays.asList(
        new Quiz(
            "Quel est le principal nutriment que l'on trouve dans les agrumes comme les oranges et les citrons ?",
            Arrays.asList("Vitamine C", "Protéine", "Fer", "Calcium"),
            0
        ),
        new Quiz(
            "Combien de minutes d'exercice d'intensité modérée est-il recommandé de faire par semaine pour un adulte ?",
            Arrays.asList("30 minutes", "90 minutes", "150 minutes", "300 minutes"),
            2
        ),
        new Quiz(
            "Laquelle de ces affirmations sur l'hydratation est VRAIE ?",
            Arrays.asList("Il faut boire 8 verres d'eau par jour, sans exception.", "Les sodas sont une bonne source d'hydratation.", "La soif est un indicateur précoce de déshydratation.", "L'eau aide à réguler la température du corps."),
            3
        ),
        new Quiz(
            "Quel est le nom du 'bon' cholestérol ?",
            Arrays.asList("LDL", "HDL", "Triglycérides", "VLDL"),
            1
        )
    );

    private static final Random random = new Random();

    public Quiz getRandomQuiz() {
        return quizzes.get(random.nextInt(quizzes.size()));
    }
}
