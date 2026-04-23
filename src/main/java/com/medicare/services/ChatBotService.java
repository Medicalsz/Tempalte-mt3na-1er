package com.medicare.services;

import com.medicare.services.ChatBotKnowledge.ScoredSpecialite;

import java.util.ArrayList;
import java.util.List;

/**
 * Moteur conversationnel du chatbot medical.
 * Implemente une machine a etats qui guide le patient :
 *  GREETING -> ASK_SYMPTOMS -> [CONFIRM_SPECIALTY] -> ASK_INTENSITY
 *           -> ASK_DURATION -> RESULT
 *
 * Retourne a chaque etape un {@link BotResponse} contenant le texte du bot
 * et eventuellement des suggestions de reponses rapides.
 */
public class ChatBotService {

    public enum State { GREETING, ASK_SYMPTOMS, CONFIRM_SPECIALTY, ASK_INTENSITY, ASK_DURATION, RESULT, END }

    public enum Urgency {
        FAIBLE("Faible", "#16a34a"),
        MOYENNE("Moyenne", "#f59e0b"),
        ELEVEE("Elevee", "#dc2626"),
        CRITIQUE("Critique - Urgence", "#7f1d1d");

        public final String label;
        public final String color;
        Urgency(String l, String c) { this.label = l; this.color = c; }
    }

    private State state = State.GREETING;
    private ChatBotKnowledge.SpecialiteEntry specialiteChoisie;
    private int intensite = 0;     // 1..5
    private int dureeJours = 0;
    private boolean urgenceDetectee = false;
    private List<ScoredSpecialite> candidats = new ArrayList<>();

    // ========== API ==========

    public State getState() { return state; }
    public ChatBotKnowledge.SpecialiteEntry getSpecialite() { return specialiteChoisie; }
    public int getIntensite() { return intensite; }
    public int getDureeJours() { return dureeJours; }

    /** Calcule l'urgence finale en fonction des reponses. */
    public Urgency computeUrgency() {
        if (urgenceDetectee) return Urgency.CRITIQUE;
        if (intensite >= 4 && dureeJours <= 1) return Urgency.ELEVEE;
        if (intensite >= 3 || dureeJours >= 14) return Urgency.MOYENNE;
        return Urgency.FAIBLE;
    }

    /** Message d'accueil initial. */
    public BotResponse start() {
        state = State.ASK_SYMPTOMS;
        return new BotResponse(
                "Bonjour ! Je suis MediBot, votre assistant medical.\n\n" +
                "Pouvez-vous me decrire vos symptomes ou ce qui vous preoccupe ?",
                List.of("J'ai mal a la tete", "J'ai de la fievre", "J'ai mal au ventre")
        );
    }

    /**
     * Traite la reponse de l'utilisateur selon l'etat courant.
     * Retourne la reponse du bot.
     */
    public BotResponse process(String userInput) {
        String input = userInput == null ? "" : userInput.trim();

        switch (state) {
            case ASK_SYMPTOMS:
                return handleSymptoms(input);
            case CONFIRM_SPECIALTY:
                return handleConfirmSpecialty(input);
            case ASK_INTENSITY:
                return handleIntensity(input);
            case ASK_DURATION:
                return handleDuration(input);
            default:
                return new BotResponse("La consultation est terminee. Cliquez sur 'Prendre RDV' pour continuer.", List.of());
        }
    }

    // ========== HANDLERS ==========

    private BotResponse handleSymptoms(String input) {
        if (ChatBotKnowledge.contientMotUrgence(input)) {
            urgenceDetectee = true;
        }

        candidats = ChatBotKnowledge.analyser(input);

        if (candidats.isEmpty()) {
            return new BotResponse(
                    "Je n'ai pas bien compris vos symptomes. Pouvez-vous reformuler ou donner plus de details ?\n" +
                    "(Exemple : 'mal de tete', 'douleur au ventre', 'toux persistante')",
                    List.of("Mal de tete", "Douleur au ventre", "Probleme de peau", "Fievre")
            );
        }

        if (candidats.size() == 1 || candidats.get(0).score > candidats.get(1).score + 1) {
            // Un seul gagnant clair
            specialiteChoisie = candidats.get(0).specialite;
            state = State.ASK_INTENSITY;
            return new BotResponse(
                    "D'apres vos symptomes, une consultation en *" + specialiteChoisie.nom +
                    "* semble indiquee.\n\n" + specialiteChoisie.questionSuivi +
                    "\n\nSur une echelle de 1 a 5, quelle est l'intensite de votre douleur / gene ?",
                    List.of("1", "2", "3", "4", "5")
            );
        } else {
            // Plusieurs spécialités en compétition : demander confirmation
            state = State.CONFIRM_SPECIALTY;
            StringBuilder sb = new StringBuilder("Plusieurs specialites pourraient convenir. Laquelle correspond le mieux ?\n\n");
            List<String> suggestions = new ArrayList<>();
            for (int i = 0; i < Math.min(3, candidats.size()); i++) {
                sb.append("- ").append(candidats.get(i).specialite.nom).append("\n");
                suggestions.add(candidats.get(i).specialite.nom);
            }
            return new BotResponse(sb.toString(), suggestions);
        }
    }

    private BotResponse handleConfirmSpecialty(String input) {
        String normalized = input.toLowerCase();
        for (ScoredSpecialite sc : candidats) {
            if (normalized.contains(sc.specialite.nom.toLowerCase())) {
                specialiteChoisie = sc.specialite;
                state = State.ASK_INTENSITY;
                return new BotResponse(
                        "Parfait, " + specialiteChoisie.nom + " choisie.\n\n" +
                        "Sur une echelle de 1 a 5, quelle est l'intensite de votre douleur / gene ?",
                        List.of("1", "2", "3", "4", "5")
                );
            }
        }
        // Par défaut : prendre le meilleur candidat
        specialiteChoisie = candidats.get(0).specialite;
        state = State.ASK_INTENSITY;
        return new BotResponse(
                "Tres bien, je vais proposer " + specialiteChoisie.nom + ".\n\n" +
                "Sur une echelle de 1 a 5, quelle est l'intensite de votre douleur / gene ?",
                List.of("1", "2", "3", "4", "5")
        );
    }

    private BotResponse handleIntensity(String input) {
        Integer n = parseInt(input);
        if (n == null || n < 1 || n > 5) {
            return new BotResponse(
                    "Veuillez indiquer un chiffre entre 1 et 5.",
                    List.of("1", "2", "3", "4", "5")
            );
        }
        intensite = n;
        state = State.ASK_DURATION;
        return new BotResponse(
                "Merci. Depuis combien de jours ressentez-vous ces symptomes ?",
                List.of("Aujourd'hui", "2-3 jours", "1 semaine", "Plus de 2 semaines")
        );
    }

    private BotResponse handleDuration(String input) {
        Integer n = parseDuration(input);
        if (n == null) {
            return new BotResponse(
                    "Pouvez-vous preciser le nombre de jours (un chiffre) ?",
                    List.of("1", "3", "7", "14", "30")
            );
        }
        dureeJours = n;
        state = State.RESULT;

        Urgency u = computeUrgency();
        String msg = "Voici mon analyse :\n\n" +
                "- Specialite recommandee : " + specialiteChoisie.nom + "\n" +
                "- Intensite : " + intensite + "/5\n" +
                "- Duree : " + dureeJours + " jour(s)\n" +
                "- Urgence : " + u.label + "\n\n";
        if (u == Urgency.CRITIQUE) {
            msg += "ATTENTION : Vos symptomes peuvent necessiter une prise en charge immediate.\n" +
                   "Contactez le SAMU (190) ou rendez-vous aux urgences.";
        } else if (u == Urgency.ELEVEE) {
            msg += "Il est recommande de consulter rapidement (dans les 24-48h).";
        } else {
            msg += "Vous pouvez prendre un rendez-vous a votre convenance.";
        }
        msg += "\n\nCliquez sur 'Prendre rendez-vous' pour continuer.";
        return new BotResponse(msg, List.of());
    }

    // ========== VALIDATION ==========

    private Integer parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return null; }
    }

    private Integer parseDuration(String s) {
        String low = s.toLowerCase().trim();
        if (low.contains("aujourd")) return 0;
        if (low.contains("hier")) return 1;
        if (low.contains("2-3") || low.contains("quelques")) return 3;
        if (low.contains("semaine")) {
            if (low.contains("2") || low.contains("deux") || low.contains("plus")) return 14;
            return 7;
        }
        if (low.contains("mois")) return 30;
        // Extraire un nombre
        StringBuilder num = new StringBuilder();
        for (char c : s.toCharArray()) if (Character.isDigit(c)) num.append(c);
        if (num.length() > 0) {
            try { return Integer.parseInt(num.toString()); } catch (Exception e) { /* ignore */ }
        }
        return null;
    }

    // ========== RESPONSE DTO ==========

    public static class BotResponse {
        public final String message;
        public final List<String> suggestions;

        public BotResponse(String message, List<String> suggestions) {
            this.message = message;
            this.suggestions = suggestions;
        }
    }
}

