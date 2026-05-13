package com.medicare.services;

import com.medicare.models.ChatAssistantRecommendation;
import com.medicare.models.ChatAssistantResponse;
import com.medicare.models.ChatMessage;
import com.medicare.models.ContentModerationResult;
import com.medicare.models.ForumComment;
import com.medicare.models.ForumTopic;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ChatAssistantService {
    private static final String MEDICAL_DISCLAIMER = "Ces informations ne remplacent pas un avis medical professionnel.";
    private static final int MAX_HISTORY_MESSAGES = 10;
    private static final int MAX_COMMENTS_IN_CONTEXT = 8;
    private static final int MAX_RELATED_TOPICS = 4;

    private static final List<String> SUMMARY_HINTS = List.of("resume", "resume-moi", "resumer", "resume", "synthese");
    private static final List<String> RELATED_HINTS = List.of("similaire", "similaires", "proches", "recommande", "suggestion", "autre sujet", "forum");
    private static final List<String> WELLNESS_HINTS = List.of("stress", "sommeil", "hydration", "alimentation", "bien-etre", "bien etre", "fatigue");
    private static final List<String> EMERGENCY_HINTS = List.of(
            "douleur thoracique", "difficulte a respirer", "convulsion", "convulsions", "perte de connaissance",
            "idee suicidaire", "idees suicidaires", "hemorragie", "saignement important", "avc", "empoisonnement"
    );
    private static final List<String> DANGEROUS_ADVICE_HINTS = List.of(
            "doublez la dose", "arretez votre traitement", "sans ordonnance",
            "ne consultez pas", "utilisez des antibiotiques restants", "prenez n'importe", "ignorez vos symptomes",
            "fabriquez", "fabrique une solution maison", "automedication agressive", "auto medication agressive"
    );

    private final OpenRouterService openRouterService = new OpenRouterService();
    private final ContentModerationService contentModerationService = new ContentModerationService();

    public void warmUp() {
        try {
            openRouterService.validateConfiguration();
        } catch (IllegalStateException ignored) {
            // The forum assistant can still operate in local fallback mode.
        }
    }

    public ChatAssistantResponse askAssistant(String message,
                                             ForumTopic topic,
                                             List<ForumComment> comments,
                                             List<ForumTopic> relatedTopics) {
        return askAssistant(message, topic, comments, relatedTopics, List.of());
    }

    public ChatAssistantResponse askAssistant(String message,
                                             ForumTopic topic,
                                             List<ForumComment> comments,
                                             List<ForumTopic> relatedTopics,
                                             List<ChatMessage> conversationHistory) {
        if (message == null || message.isBlank()) {
            throw new IllegalStateException("Le message du chatbot ne peut pas etre vide.");
        }
        if (topic == null) {
            throw new IllegalStateException("Le sujet du forum est introuvable.");
        }

        String trimmedMessage = message.trim();
        ContentModerationResult moderationResult = contentModerationService.moderateLocally(trimmedMessage);
        if (moderationResult.hasToxicContent() || containsAny(normalize(trimmedMessage), EMERGENCY_HINTS)) {
            return buildProtectedResponse(moderationResult, relatedTopics);
        }

        AssistantIntent intent = detectIntent(trimmedMessage);
        String prompt = buildPrompt(trimmedMessage, topic, comments, relatedTopics, conversationHistory, intent);

        try {
            OpenRouterService.ChatCompletionResult apiResult = openRouterService.sendMessage(prompt);
            String safeReply = postProcessReply(apiResult.reply());

            if (containsAny(normalize(safeReply), DANGEROUS_ADVICE_HINTS)) {
                safeReply = buildSafetyFallback();
            }

            ChatAssistantResponse response = new ChatAssistantResponse();
            response.setReply(safeReply);
            response.setIntent(intent.name().toLowerCase(Locale.ROOT));
            response.setConfidence(0.92d);
            response.setFallbackResponse(false);
            response.setModel(apiResult.model());
            response.setRecommendations(selectRecommendations(intent, relatedTopics, trimmedMessage));
            return response;
        } catch (IllegalStateException openAiFailure) {
            return buildLocalFallbackResponse(trimmedMessage, topic, comments, relatedTopics, intent, openAiFailure.getMessage());
        }
    }

    private ChatAssistantResponse buildProtectedResponse(ContentModerationResult moderationResult, List<ForumTopic> relatedTopics) {
        ChatAssistantResponse response = new ChatAssistantResponse();
        StringBuilder builder = new StringBuilder();
        if (moderationResult != null && moderationResult.hasToxicContent() && moderationResult.getMessage() != null && !moderationResult.getMessage().isBlank()) {
            builder.append(moderationResult.getMessage()).append('\n').append('\n');
        } else {
            builder.append("Je ne peux pas aider sur une demande potentiellement dangereuse ou urgente. ")
                    .append("Contactez rapidement un professionnel de sante ou les urgences si la situation est immediate.")
                    .append('\n').append('\n');
        }
        builder.append(MEDICAL_DISCLAIMER);

        response.setReply(builder.toString());
        response.setIntent(AssistantIntent.SAFETY.name().toLowerCase(Locale.ROOT));
        response.setConfidence(1.0d);
        response.setFallbackResponse(true);
        response.setModel(OpenRouterService.DEFAULT_MODEL);
        response.setRecommendations(selectRecommendations(AssistantIntent.RELATED, relatedTopics, "similaire"));
        return response;
    }

    private ChatAssistantResponse buildLocalFallbackResponse(String message,
                                                             ForumTopic topic,
                                                             List<ForumComment> comments,
                                                             List<ForumTopic> relatedTopics,
                                                             AssistantIntent intent,
                                                             String cause) {
        ChatAssistantResponse response = new ChatAssistantResponse();
        response.setReply(postProcessReply(buildLocalReply(message, topic, comments, relatedTopics, intent, cause)));
        response.setIntent(intent.name().toLowerCase(Locale.ROOT));
        response.setConfidence(0.68d);
        response.setFallbackResponse(true);
        response.setModel("local-medical-fallback");
        response.setRecommendations(selectRecommendations(intent, relatedTopics, message));
        return response;
    }

    private String buildLocalReply(String message,
                                   ForumTopic topic,
                                   List<ForumComment> comments,
                                   List<ForumTopic> relatedTopics,
                                   AssistantIntent intent,
                                   String cause) {
        StringBuilder builder = new StringBuilder();
        builder.append("Mode local Medicare actif");
        if (cause != null && !cause.isBlank()) {
            builder.append(" car l'API externe gratuite n'est pas disponible pour ce projet pour le moment.");
        } else {
            builder.append(".");
        }
        builder.append("\n\n");

        switch (intent) {
            case SUMMARY -> builder.append(buildLocalSummary(topic, comments));
            case RELATED -> builder.append(buildLocalRelatedTopics(topic, relatedTopics));
            case WELLNESS -> builder.append(buildLocalWellnessAdvice(message, topic, comments));
            case GENERAL -> builder.append(buildLocalGeneralHealthAnswer(message, topic, comments, relatedTopics));
            case SAFETY -> builder.append(buildSafetyFallback());
        }

        return builder.toString();
    }

    private String buildLocalSummary(ForumTopic topic, List<ForumComment> comments) {
        List<String> bulletPoints = new ArrayList<>();
        bulletPoints.add("Le sujet porte surtout sur \"" + safeValue(topic.getTitle()) + "\".");

        String summary = topic.getDisplaySummary();
        if (summary != null && !summary.isBlank()) {
            bulletPoints.add("L'idee principale est : " + compact(summary, 180) + ".");
        } else {
            bulletPoints.add("Le contenu met en avant : " + compact(topic.getContent(), 180) + ".");
        }

        String commentPulse = buildCommentPulse(comments);
        if (!commentPulse.isBlank()) {
            bulletPoints.add(commentPulse);
        } else if (topic.getTagsDisplay() != null && !topic.getTagsDisplay().isBlank()) {
            bulletPoints.add("Les themes associes sont : " + topic.getTagsDisplay() + ".");
        } else {
            bulletPoints.add("C'est un sujet de type " + safeValue(topic.getDisplayType()).toLowerCase(Locale.ROOT) + " partage sur le forum.");
        }

        StringBuilder builder = new StringBuilder("Voici un resume en 3 points :\n");
        for (String bulletPoint : bulletPoints) {
            builder.append("- ").append(cleanSentenceEnding(bulletPoint)).append('\n');
        }
        return builder.toString().trim();
    }

    private String buildLocalRelatedTopics(ForumTopic topic, List<ForumTopic> relatedTopics) {
        StringBuilder builder = new StringBuilder("Je peux deja vous orienter vers des sujets proches de \"")
                .append(safeValue(topic.getTitle()))
                .append("\" :\n");

        if (relatedTopics == null || relatedTopics.isEmpty()) {
            builder.append("- Aucun sujet similaire n'est remonte pour le moment.\n");
            builder.append("- Vous pouvez aussi explorer les tags de ce sujet : ").append(safeValue(topic.getTagsDisplay())).append(".\n");
            builder.append("- Si vous voulez, je peux aussi resumer la discussion actuelle.");
            return builder.toString();
        }

        int count = 0;
        for (ForumTopic relatedTopic : relatedTopics) {
            if (relatedTopic == null || relatedTopic.getTitle() == null || relatedTopic.getTitle().isBlank()) {
                continue;
            }

            builder.append("- ").append(relatedTopic.getTitle());
            if (relatedTopic.getTagsDisplay() != null && !relatedTopic.getTagsDisplay().isBlank()) {
                builder.append(" (tags : ").append(relatedTopic.getTagsDisplay()).append(")");
            }
            builder.append('\n');
            count++;
            if (count >= 4) {
                break;
            }
        }

        builder.append("Je peux aussi resumer celui-ci ou vous dire pourquoi ces sujets se ressemblent.");
        return builder.toString().trim();
    }

    private String buildLocalWellnessAdvice(String message, ForumTopic topic, List<ForumComment> comments) {
        String normalized = normalize(message + " " + safeValue(topic.getTitle()) + " " + safeValue(topic.getTagsDisplay()));
        StringBuilder builder = new StringBuilder("Voici quelques conseils simples et prudents :\n");

        if (normalized.contains("stress") || normalized.contains("anxiete") || normalized.contains("mental")) {
            builder.append("- Essayez de faire de courtes pauses, de ralentir la respiration et de structurer la journee avec des priorites realistes.\n");
            builder.append("- Un sommeil regulier, moins d'ecrans le soir et un peu de marche ou d'etirements peuvent aider a diminuer la tension.\n");
            builder.append("- Si le stress devient constant, tres intense ou bloque vos activites, il vaut mieux en parler a un professionnel de sante.\n");
        } else if (normalized.contains("sommeil")) {
            builder.append("- Gardez des horaires de coucher assez stables, meme le week-end.\n");
            builder.append("- Limitez cafe, boissons energisantes et telephone juste avant de dormir.\n");
            builder.append("- Si les troubles du sommeil durent ou s'aggravent, demandez un avis medical personnalise.\n");
        } else if (normalized.contains("alimentation") || normalized.contains("nutrition")) {
            builder.append("- Visez des repas reguliers, une bonne hydratation et des aliments simples plutot que des changements extremes.\n");
            builder.append("- Evitez l'automedication ou les complements pris sans avis adapte a votre situation.\n");
            builder.append("- En cas de perte de poids, douleurs digestives ou malaise associe, consultez un professionnel de sante.\n");
        } else {
            builder.append("- Reposez-vous suffisamment, hydratez-vous bien et surveillez l'evolution des symptomes dans le temps.\n");
            builder.append("- Evitez les conseils extremes, les doses improvisees ou l'arret d'un traitement sans avis professionnel.\n");
            builder.append("- Si un symptome devient fort, inhabituel ou inquietant, il vaut mieux demander un avis medical reel.\n");
        }

        String commentPulse = buildCommentPulse(comments);
        if (!commentPulse.isBlank()) {
            builder.append("\nDans cette discussion, ").append(lowercaseFirst(commentPulse));
        }
        return builder.toString().trim();
    }

    private String buildLocalGeneralHealthAnswer(String message,
                                                 ForumTopic topic,
                                                 List<ForumComment> comments,
                                                 List<ForumTopic> relatedTopics) {
        String normalized = normalize(message + " " + safeValue(topic.getTitle()) + " " + safeValue(topic.getContent()));
        StringBuilder builder = new StringBuilder();

        if (normalized.contains("stress") || normalized.contains("anxiete")) {
            builder.append("Sur ce theme, on parle surtout de gestion du stress et d'equilibre mental. ");
            builder.append("Des approches simples comme la respiration lente, l'organisation du travail, le sommeil regulier et une activite physique douce peuvent aider dans beaucoup de situations.");
        } else if (normalized.contains("fatigue")) {
            builder.append("La fatigue peut avoir des causes tres variees : sommeil insuffisant, stress, rythme trop charge, alimentation, ou autre probleme de sante. ");
            builder.append("Observer depuis quand elle dure, ce qui l'aggrave et les signes associes aide deja a mieux orienter la suite.");
        } else if (normalized.contains("sommeil")) {
            builder.append("Le sujet fait penser a une question de sommeil ou de recuperation. ");
            builder.append("Une routine stable, moins d'ecrans le soir et un environnement calme peuvent etre utiles avant de chercher des solutions plus fortes.");
        } else {
            builder.append("A partir de ce sujet, je peux donner une orientation generale mais pas un diagnostic. ");
            builder.append("Le plus utile est de regarder les symptomes, leur duree, leur intensite et les facteurs qui les declenchent.");
        }

        String summary = topic.getDisplaySummary();
        if (summary != null && !summary.isBlank()) {
            builder.append("\n\nDans le sujet actuel, l'idee centrale est : ").append(compact(summary, 170)).append(".");
        }

        String commentPulse = buildCommentPulse(comments);
        if (!commentPulse.isBlank()) {
            builder.append("\n").append(commentPulse);
        }

        if (relatedTopics != null && !relatedTopics.isEmpty()) {
            ForumTopic firstRelated = relatedTopics.getFirst();
            if (firstRelated != null && firstRelated.getTitle() != null && !firstRelated.getTitle().isBlank()) {
                builder.append("\n\nSi vous voulez poursuivre, je peux aussi vous rapprocher du sujet similaire \"")
                        .append(firstRelated.getTitle())
                        .append("\".");
            }
        }

        return builder.toString().trim();
    }

    private String buildCommentPulse(List<ForumComment> comments) {
        if (comments == null || comments.isEmpty()) {
            return "";
        }

        List<String> visibleComments = new ArrayList<>();
        for (ForumComment comment : comments) {
            if (comment == null || comment.isHidden() || comment.getContent() == null || comment.getContent().isBlank()) {
                continue;
            }
            visibleComments.add(compact(comment.getContent(), 120));
            if (visibleComments.size() >= 2) {
                break;
            }
        }

        if (visibleComments.isEmpty()) {
            return "";
        }
        if (visibleComments.size() == 1) {
            return "un commentaire met en avant : " + visibleComments.getFirst() + ".";
        }
        return "les commentaires insistent surtout sur : " + visibleComments.getFirst() + " / " + visibleComments.get(1) + ".";
    }

    private String buildPrompt(String message,
                               ForumTopic topic,
                               List<ForumComment> comments,
                               List<ForumTopic> relatedTopics,
                               List<ChatMessage> conversationHistory,
                               AssistantIntent intent) {
        StringBuilder builder = new StringBuilder();
        builder.append("Tu es un assistant medical educatif integre au forum Medicare.\n");
        builder.append("Tu reponds en francais, de facon conversationnelle, chaleureuse et concise.\n");
        builder.append("Tu peux repondre a des questions medicales generales, resumer le sujet du forum, proposer des conseils bien-etre simples et recommander des sujets similaires.\n");
        builder.append("Regles obligatoires:\n");
        builder.append("- N'etablis jamais de diagnostic definitif.\n");
        builder.append("- Ne prescris pas de traitement, de posologie, ni d'automedication risquee.\n");
        builder.append("- Si une urgence ou un danger apparait, oriente immediatement vers un professionnel de sante ou les urgences.\n");
        builder.append("- Ne promets jamais de guerison.\n");
        builder.append("- Termine TOUJOURS la reponse par la phrase exacte: ").append(MEDICAL_DISCLAIMER).append("\n");
        builder.append("- Si la demande concerne des sujets similaires, cite prioritairement les titres du forum fournis dans le contexte.\n");
        builder.append("- Si un resume est demande, fais une synthese claire du sujet courant avant toute suggestion.\n\n");

        builder.append("Type de demande detecte: ").append(intent.description).append("\n\n");
        builder.append("Sujet forum courant:\n");
        builder.append("Titre: ").append(safeValue(topic.getTitle())).append("\n");
        builder.append("Type: ").append(safeValue(topic.getDisplayType())).append("\n");
        builder.append("Resume existant: ").append(safeValue(topic.getDisplaySummary())).append("\n");
        builder.append("Contenu principal: ").append(safeValue(compact(topic.getContent(), 1800))).append("\n");
        builder.append("Tags: ").append(safeValue(topic.getTagsDisplay())).append("\n\n");

        builder.append("Commentaires du sujet:\n");
        appendComments(builder, comments);
        builder.append('\n');

        builder.append("Sujets similaires deja trouves dans le forum:\n");
        appendRelatedTopics(builder, relatedTopics);
        builder.append('\n');

        builder.append("Historique recent de la conversation:\n");
        appendConversationHistory(builder, conversationHistory);
        builder.append('\n');

        builder.append("Nouvelle demande de l'utilisateur:\n");
        builder.append(message).append('\n').append('\n');
        builder.append("Format attendu:\n");
        builder.append("- 1 a 3 courts paragraphes maximum.\n");
        builder.append("- Ajoute de petites puces seulement si cela clarifie la reponse.\n");
        builder.append("- Reste prudent et utile.\n");
        return builder.toString();
    }

    private void appendComments(StringBuilder builder, List<ForumComment> comments) {
        if (comments == null || comments.isEmpty()) {
            builder.append("- Aucun commentaire utile pour le moment.\n");
            return;
        }

        int count = 0;
        for (ForumComment comment : comments) {
            if (comment == null || comment.getContent() == null || comment.getContent().isBlank() || comment.isHidden()) {
                continue;
            }

            builder.append("- ")
                    .append(safeValue(comment.getAuthorName()))
                    .append(": ")
                    .append(safeValue(compact(comment.getContent(), 220)))
                    .append('\n');

            count++;
            if (count >= MAX_COMMENTS_IN_CONTEXT) {
                break;
            }
        }

        if (count == 0) {
            builder.append("- Aucun commentaire utile pour le moment.\n");
        }
    }

    private void appendRelatedTopics(StringBuilder builder, List<ForumTopic> relatedTopics) {
        if (relatedTopics == null || relatedTopics.isEmpty()) {
            builder.append("- Aucun sujet similaire disponible.\n");
            return;
        }

        int count = 0;
        for (ForumTopic relatedTopic : relatedTopics) {
            if (relatedTopic == null || relatedTopic.getTitle() == null || relatedTopic.getTitle().isBlank()) {
                continue;
            }

            builder.append("- [ID ").append(relatedTopic.getId()).append("] ")
                    .append(safeValue(relatedTopic.getTitle()))
                    .append(" | Tags: ").append(safeValue(relatedTopic.getTagsDisplay()))
                    .append(" | Resume: ").append(safeValue(compact(relatedTopic.getDisplaySummary(), 140)))
                    .append('\n');

            count++;
            if (count >= MAX_RELATED_TOPICS) {
                break;
            }
        }

        if (count == 0) {
            builder.append("- Aucun sujet similaire disponible.\n");
        }
    }

    private void appendConversationHistory(StringBuilder builder, List<ChatMessage> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            builder.append("- Debut de conversation.\n");
            return;
        }

        List<ChatMessage> eligibleMessages = conversationHistory.stream()
                .filter(ChatMessage::shouldIncludeInPrompt)
                .toList();

        if (eligibleMessages.isEmpty()) {
            builder.append("- Debut de conversation.\n");
            return;
        }

        int startIndex = Math.max(0, eligibleMessages.size() - MAX_HISTORY_MESSAGES);
        for (int i = startIndex; i < eligibleMessages.size(); i++) {
            ChatMessage chatMessage = eligibleMessages.get(i);
            builder.append("- ")
                    .append(chatMessage.isUser() ? "Utilisateur" : "Assistant")
                    .append(": ")
                    .append(safeValue(compact(chatMessage.getContent(), 260)))
                    .append('\n');
        }
    }

    private AssistantIntent detectIntent(String message) {
        String normalized = normalize(message);
        if (containsAny(normalized, SUMMARY_HINTS)) {
            return AssistantIntent.SUMMARY;
        }
        if (containsAny(normalized, RELATED_HINTS)) {
            return AssistantIntent.RELATED;
        }
        if (containsAny(normalized, WELLNESS_HINTS)) {
            return AssistantIntent.WELLNESS;
        }
        return AssistantIntent.GENERAL;
    }

    private String postProcessReply(String reply) {
        String sanitized = compact(reply, 2400).trim();
        if (sanitized.isEmpty()) {
            sanitized = "Je n'ai pas pu formuler une reponse exploitable pour le moment.";
        }

        if (!normalize(sanitized).contains(normalize(MEDICAL_DISCLAIMER))) {
            sanitized = sanitized + "\n\n" + MEDICAL_DISCLAIMER;
        }
        return sanitized;
    }

    private String buildSafetyFallback() {
        return "Je prefere rester prudent sur ce point. Pour ce type de situation, le plus sur est de demander un avis medical personnalise plutot que de suivre un conseil potentiellement risque.\n\n"
                + MEDICAL_DISCLAIMER;
    }

    private List<ChatAssistantRecommendation> selectRecommendations(AssistantIntent intent,
                                                                    List<ForumTopic> relatedTopics,
                                                                    String message) {
        if (relatedTopics == null || relatedTopics.isEmpty()) {
            return List.of();
        }

        String normalized = normalize(message);
        boolean shouldSuggest = intent == AssistantIntent.RELATED
                || intent == AssistantIntent.SUMMARY
                || normalized.contains("forum")
                || normalized.contains("sujet");

        if (!shouldSuggest) {
            return List.of();
        }

        List<ChatAssistantRecommendation> recommendations = new ArrayList<>();
        int limit = intent == AssistantIntent.RELATED ? 4 : 2;
        for (ForumTopic relatedTopic : relatedTopics) {
            if (relatedTopic == null || relatedTopic.getTitle() == null || relatedTopic.getTitle().isBlank()) {
                continue;
            }
            recommendations.add(new ChatAssistantRecommendation(relatedTopic.getId(), relatedTopic.getTitle()));
            if (recommendations.size() >= limit) {
                break;
            }
        }
        return recommendations;
    }

    private boolean containsAny(String normalizedValue, List<String> candidates) {
        for (String candidate : candidates) {
            if (normalizedValue.contains(normalize(candidate))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String compact(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.length() <= maxLength ? compact : compact.substring(0, maxLength - 3) + "...";
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private String cleanSentenceEnding(String value) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.endsWith(".") || cleaned.endsWith("!") || cleaned.endsWith("?")) {
            return cleaned;
        }
        return cleaned + ".";
    }

    private String lowercaseFirst(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private enum AssistantIntent {
        GENERAL("question medicale generale"),
        SUMMARY("resume du sujet"),
        RELATED("recommandation de sujets similaires"),
        WELLNESS("conseils bien-etre"),
        SAFETY("blocage de securite");

        private final String description;

        AssistantIntent(String description) {
            this.description = description;
        }
    }
}
