package com.example.jawafai.model

/**
 * Represents a question in the persona settings.
 *
 * @property id The unique identifier for the question, used as the key in Firebase
 * @property prompt The text of the question to display to the user
 * @property type The type of question (single choice or free text)
 * @property options Available options for single choice questions, null for free text
 * @property required Whether the question requires an answer
 */
data class PersonaQuestion(
    val id: String,
    val prompt: String,
    val type: QuestionType,
    val options: List<String>? = null,
    val required: Boolean = true
)

/**
 * Defines the types of questions available in the persona settings.
 */
enum class QuestionType {
    SINGLE_CHOICE,
    FREE_TEXT
}

/**
 * Predefined set of persona questions for the application.
 */
object PersonaQuestions {
    val questions = listOf(
        PersonaQuestion(
            id = "communication_style",
            prompt = "How would you describe your communication style?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf("Direct and brief", "Detailed and thorough", "Casual and friendly", "Formal and professional")
        ),
        PersonaQuestion(
            id = "decision_making",
            prompt = "How do you typically make decisions?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf("Analytical and logical", "Based on feelings and intuition", "Considering others' opinions", "Quick and decisive")
        ),
        PersonaQuestion(
            id = "learning_preference",
            prompt = "What's your preferred learning style?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf("Visual (seeing)", "Auditory (hearing)", "Reading/Writing", "Kinesthetic (doing)")
        ),
        PersonaQuestion(
            id = "interests",
            prompt = "What are your main interests or hobbies?",
            type = QuestionType.FREE_TEXT
        ),
        PersonaQuestion(
            id = "goals",
            prompt = "What are your current personal or professional goals?",
            type = QuestionType.FREE_TEXT
        ),
        PersonaQuestion(
            id = "challenges",
            prompt = "What challenges are you currently facing?",
            type = QuestionType.FREE_TEXT
        ),
        PersonaQuestion(
            id = "ideal_day",
            prompt = "Describe your ideal day in a few sentences.",
            type = QuestionType.FREE_TEXT
        ),
        PersonaQuestion(
            id = "stress_response",
            prompt = "How do you typically respond to stress?",
            type = QuestionType.SINGLE_CHOICE,
            options = listOf("Take action and solve problems", "Seek support from others", "Take time to reflect", "Distract myself with activities")
        )
    )
}
