package com.medicare.services;

import java.util.*;

/**
 * Base de connaissances locale du chatbot Medicare.
 * Mappe des mots-clés (symptômes, organes, douleurs) vers des spécialités,
 * et définit les règles de niveau d'urgence.
 *
 * Organisée comme une "mini base de données" en mémoire, facile à enrichir.
 */
public class ChatBotKnowledge {

    /** Une spécialité avec sa liste de mots-clés déclencheurs. */
    public static class SpecialiteEntry {
        public final String nom;          // Doit correspondre EXACTEMENT au nom en BDD
        public final List<String> keywords;
        public final String questionSuivi; // Question de suivi posée au patient

        public SpecialiteEntry(String nom, String questionSuivi, String... keywords) {
            this.nom = nom;
            this.questionSuivi = questionSuivi;
            this.keywords = Arrays.asList(keywords);
        }
    }

    /** Base des spécialités + mots-clés associés. */
    private static final List<SpecialiteEntry> SPECIALITES = new ArrayList<>();

    /** Mots-clés d'urgence (déclenchent un niveau d'urgence élevé). */
    private static final Set<String> MOTS_URGENCE = new HashSet<>(Arrays.asList(
            "urgent", "urgence", "insupportable", "tres fort", "très fort",
            "sang", "saigne", "saignement", "saignement abondant",
            "evanouissement", "évanouissement", "evanoui", "évanoui",
            "ne respire plus", "respire mal", "etouffe", "étouffe",
            "paralyse", "paralysé", "perte de conscience", "convulsion",
            "douleur thoracique", "douleur poitrine", "crise cardiaque",
            "avc", "accident vasculaire", "grave", "tres grave", "très grave"
    ));

    static {
        // === Cardiologie ===
        SPECIALITES.add(new SpecialiteEntry("Cardiologie",
                "Avez-vous des essoufflements ou des palpitations ?",
                "coeur", "cœur", "cardiaque", "cardio", "poitrine", "thorax",
                "palpitation", "palpitations", "tension", "hypertension", "hypotension",
                "pouls", "thoracique", "arythmie", "essoufflement", "souffle",
                "infarctus", "angine de poitrine", "pacemaker", "cholesterol",
                "cholestérol", "serrement poitrine", "pincement coeur"));

        // === Neurologie ===
        SPECIALITES.add(new SpecialiteEntry("Neurologie",
                "Avez-vous des vertiges ou des pertes d'equilibre ?",
                "tete", "tête", "crane", "crâne", "migraine", "migraines",
                "vertige", "vertiges", "cephalee", "céphalée", "mal de tete",
                "mal de tête", "neurologique", "paralysie", "tremblement", "tremblements",
                "convulsion", "convulsions", "epilepsie", "épilepsie", "crise epilepsie",
                "memoire", "mémoire", "oubli", "oublis", "evanouissement", "évanouissement",
                "engourdissement", "fourmillement", "fourmillements", "sclerose en plaques",
                "sclérose en plaques", "parkinson", "alzheimer", "nerf", "nerfs",
                "picotement", "picotements", "nevralgie", "névralgie"));

        // === Dermatologie ===
        SPECIALITES.add(new SpecialiteEntry("Dermatologie",
                "Y a-t-il des demangeaisons ou une eruption cutanee ?",
                "peau", "cutane", "cutané", "cutanee", "cutanée", "epiderme", "épiderme",
                "bouton", "boutons", "eczema", "eczéma", "acne", "acné",
                "demangeaison", "démangeaison", "demangeaisons", "démangeaisons",
                "qui gratte", "rougeur", "rougeurs", "eruption", "éruption",
                "grain de beaute", "grain de beauté", "tache", "taches", "dermato",
                "dermatologique", "urticaire", "psoriasis", "verrue", "verrues",
                "mycose", "champignon peau", "zona", "herpes", "herpès", "brulure peau",
                "brûlure peau", "cicatrice", "gale", "teigne", "melanome", "mélanome"));

        // === Ophtalmologie ===
        SPECIALITES.add(new SpecialiteEntry("Ophtalmologie",
                "Votre vision est-elle floue ou doublee ?",
                // Organes et bases
                "oeil", "œil", "yeux", "oculaire", "ophtalmo", "ophtalmologique",
                "paupiere", "paupière", "paupieres", "paupières", "cil", "cils",
                "pupille", "iris", "cornee", "cornée", "retine", "rétine",
                "cristallin", "macula",
                // Vision et symptomes
                "vue", "vision", "voir", "voit", "voit flou", "voit pas", "voit mal",
                "floue", "flou", "trouble", "double vision", "vision double", "diplopie",
                "vision floue", "vision trouble", "tache devant", "taches devant yeux",
                "mouches volantes", "corps flottants", "eclair", "éclair", "eclairs",
                // Inflammation / gene
                "larmoie", "larmoyant", "larme", "larmes", "oeil qui pleure",
                "oeil rouge", "yeux rouges", "rougeur yeux", "conjonctivite",
                "picotement yeux", "brulure yeux", "brûlure yeux", "demangeaison yeux",
                "secheresse oculaire", "sécheresse oculaire", "yeux secs",
                "photophobie", "sensible lumiere", "sensible à la lumière",
                "gene yeux", "gêne yeux", "corps etranger oeil", "mal aux yeux",
                // Pathologies
                "lunette", "lunettes", "verres correcteurs", "lentille", "lentilles",
                "presbytie", "myopie", "hypermetropie", "hypermétropie", "astigmatisme",
                "cataracte", "glaucome", "dmla", "degenerescence maculaire",
                "dégénérescence maculaire", "strabisme", "louche", "daltonisme",
                "orgelet", "chalazion", "blepharite", "blépharite",
                "decollement retine", "décollement rétine", "uveite", "uvéite",
                "keratite", "kératite", "pression oculaire", "tension oculaire",
                "operation yeux", "laser yeux", "cornee", "amblyopie"));

        // === ORL ===
        SPECIALITES.add(new SpecialiteEntry("ORL",
                "Avez-vous de la fievre ou une gene pour avaler ?",
                "gorge", "mal gorge", "oreille", "oreilles", "mal oreille",
                "nez", "mal nez", "sinusite", "sinus", "sinusites",
                "angine", "amygdale", "amygdales", "amygdalite", "pharyngite",
                "laryngite", "enrouement", "voix", "audition", "entend mal",
                "sourd", "surdite", "surdité", "sifflement oreille", "acouphene",
                "acouphène", "acouphenes", "acouphènes", "vertige positionnel",
                "rhume", "otite", "otites", "saignement nez", "saigne du nez",
                "ronflement", "apnee sommeil", "apnée sommeil", "ronfle",
                "difficulte avaler", "difficulté avaler"));

        // === Pneumologie ===
        SPECIALITES.add(new SpecialiteEntry("Pneumologie",
                "Depuis combien de temps avez-vous cette toux ?",
                "toux", "tousse", "tousser", "poumon", "poumons", "pulmonaire",
                "respirer", "respire", "respiration", "essouffle", "essoufflé",
                "essoufflement", "bronchite", "bronchites", "asthme", "asthmatique",
                "pneumonie", "pneumopathie", "crachat", "crachats", "glaires",
                "tuberculose", "apnee", "apnée", "bronches", "expectoration",
                "sifflement poumon", "oppression poitrine", "souffle court",
                "respiration difficile", "manque air", "covid", "bpco", "emphyseme",
                "emphysème", "tabac", "fumeur"));

        // === Gastro-entérologie ===
        SPECIALITES.add(new SpecialiteEntry("Gastro-enterologie",
                "Avez-vous des nausees ou des vomissements ?",
                "ventre", "abdomen", "abdominal", "abdominale", "estomac", "digestion",
                "indigestion", "nausee", "nausée", "nausees", "nausées",
                "vomi", "vomit", "vomissement", "vomissements", "diarrhee", "diarrhée",
                "constipation", "constipe", "constipé", "foie", "hepatique", "hépatique",
                "intestin", "intestins", "colon", "côlon", "ulcere", "ulcère",
                "gastrite", "gastro", "brulure estomac", "brûlure estomac",
                "brulure ventre", "reflux", "rgo", "ballonnement", "ballonnements",
                "gaz", "flatulence", "hemorroide", "hémorroïde", "hemorroides",
                "hémorroïdes", "sang dans selles", "diarrhee sanglante", "crohn",
                "colite", "pancreas", "pancréas", "vesicule", "vésicule",
                "appendicite", "mal ventre", "mal estomac"));

        // === Gynécologie ===
        SPECIALITES.add(new SpecialiteEntry("Gynecologie",
                "Cela concerne-t-il une grossesse ou le cycle menstruel ?",
                "regle", "règle", "regles", "règles", "menstruation", "menstruel",
                "menstruelle", "cycle", "grossesse", "enceinte", "ovaire", "ovaires",
                "uterus", "utérus", "vagin", "vaginal", "vaginale", "vulve",
                "sein", "seins", "menopause", "ménopause", "gynecologique",
                "gynécologique", "gyneco", "gynéco", "contraception", "pilule",
                "sterilet", "stérilet", "preservatif", "préservatif",
                "infection urinaire femme", "mycose vaginale", "frottis",
                "mammographie", "endometriose", "endométriose", "fibrome", "kyste ovaire",
                "saignement", "pertes blanches", "pertes", "papillomavirus", "hpv"));

        // === Pédiatrie ===
        SPECIALITES.add(new SpecialiteEntry("Pediatrie",
                "Quel age a l'enfant ?",
                "enfant", "enfants", "bebe", "bébé", "bebes", "bébés",
                "nourrisson", "nourrissons", "nouveau-ne", "nouveau né", "nouveau-né",
                "pediatrie", "pédiatrie", "pediatre", "pédiatre", "vaccin enfant",
                "fievre enfant", "fièvre enfant", "ma fille", "mon fils",
                "ma petite", "mon petit", "croissance enfant", "poids enfant",
                "autisme enfant", "eczema bebe", "varicelle", "rougeole",
                "oreillons", "scarlatine"));

        // === Rhumatologie / Orthopédie ===
        SPECIALITES.add(new SpecialiteEntry("Rhumatologie",
                "Avez-vous une raideur ou un gonflement ?",
                "os", "osseux", "articulation", "articulations", "articulaire",
                "genou", "genoux", "hanche", "hanches", "epaule", "épaule", "épaules",
                "coude", "coudes", "poignet", "poignets", "cheville", "chevilles",
                "arthrose", "arthrite", "rhumatisme", "rhumatismes", "rhumatologique",
                "lombalgie", "dos", "mal de dos", "mal au dos", "colonne vertebrale",
                "colonne vertébrale", "vertebre", "vertèbre", "vertebres", "vertèbres",
                "sciatique", "lumbago", "hernie discale", "hernie", "entorse",
                "fracture", "cassé", "casse", "tendinite", "bursite", "goutte",
                "osteoporose", "ostéoporose", "polyarthrite", "spondylarthrite",
                "fibromyalgie", "nuque", "cervicale", "cervicales", "cervicalgie",
                "raideur", "gonflement articulation", "craquement"));

        // === Psychiatrie ===
        SPECIALITES.add(new SpecialiteEntry("Psychiatrie",
                "Ces symptomes affectent-ils votre vie quotidienne ?",
                "stress", "stresse", "anxiete", "anxiété", "anxieux", "anxieuse",
                "depression", "dépression", "deprime", "déprimé", "deprimee", "déprimée",
                "triste", "tristesse", "pleure", "pleurs", "angoisse", "angoisses",
                "crise de panique", "crise panique", "panique", "insomnie",
                "dort pas", "dors pas", "sommeil", "cauchemar", "cauchemars",
                "phobie", "phobies", "peur", "peurs", "suicide", "suicidaire",
                "idees noires", "idées noires", "envie mourir", "burnout", "burn-out",
                "psychologique", "psychologue", "psychiatre", "mental", "moral",
                "deprime", "tdah", "bipolaire", "schizophrenie", "schizophrénie",
                "obsessionnel", "toc", "fatigue mentale", "surmenage", "addiction",
                "alcool", "drogue"));

        // === Urologie ===
        SPECIALITES.add(new SpecialiteEntry("Urologie",
                "Ressentez-vous une douleur en urinant ?",
                "urine", "urines", "uriner", "urinaire", "uretre", "urètre",
                "prostate", "prostatique", "vessie", "rein", "reins", "renal", "rénal",
                "renaux", "rénaux", "calcul renal", "calcul rénal", "colique nephretique",
                "colique néphrétique", "cystite", "infection urinaire", "brulure urine",
                "brûlure urine", "sang urine", "hematurie", "hématurie", "incontinence",
                "impuissance", "dysfonction erectile", "érectile", "testicule",
                "testicules", "varicocele", "varicocèle", "orchite", "circoncision"));

        // === Médecine Générale (fallback) ===
        SPECIALITES.add(new SpecialiteEntry("Medecine Generale",
                "Avez-vous de la fievre ?",
                "fievre", "fièvre", "fievres", "fièvres", "fatigue", "fatigué",
                "fatiguee", "fatiguée", "epuise", "épuisé", "grippe", "rhume",
                "malade", "maladie", "general", "général", "generaliste", "généraliste",
                "bilan", "bilan de sante", "bilan de santé", "check-up", "checkup",
                "vaccination", "vaccin", "certificat medical", "certificat médical",
                "arret maladie", "arrêt maladie", "consultation", "prise de sang",
                "analyse", "analyses", "perte de poids", "prise de poids",
                "transpiration", "sueur", "sueurs nocturnes", "frisson", "frissons",
                "mal partout", "courbature", "courbatures", "vertiges sans cause"));
    }

    public static List<SpecialiteEntry> getAllSpecialites() { return SPECIALITES; }
    public static Set<String> getMotsUrgence() { return MOTS_URGENCE; }

    /**
     * Analyse un texte utilisateur et retourne les 3 spécialités les plus probables
     * (triées par score décroissant).
     */
    public static List<ScoredSpecialite> analyser(String texte) {
        String t = normaliser(texte);
        List<ScoredSpecialite> results = new ArrayList<>();

        for (SpecialiteEntry s : SPECIALITES) {
            int score = 0;
            for (String kw : s.keywords) {
                if (t.contains(normaliser(kw))) score++;
            }
            if (score > 0) results.add(new ScoredSpecialite(s, score));
        }
        results.sort((a, b) -> Integer.compare(b.score, a.score));
        return results.size() > 3 ? results.subList(0, 3) : results;
    }

    /** Retourne vrai si le texte contient au moins un mot-clé d'urgence. */
    public static boolean contientMotUrgence(String texte) {
        String t = normaliser(texte);
        for (String mot : MOTS_URGENCE) {
            if (t.contains(normaliser(mot))) return true;
        }
        return false;
    }

    /** Normalise : minuscule + suppression d'accents basiques + trim. */
    private static String normaliser(String s) {
        if (s == null) return "";
        return s.toLowerCase()
                .replace('é', 'e').replace('è', 'e').replace('ê', 'e').replace('ë', 'e')
                .replace('à', 'a').replace('â', 'a').replace('ä', 'a')
                .replace('î', 'i').replace('ï', 'i')
                .replace('ô', 'o').replace('ö', 'o')
                .replace('ù', 'u').replace('û', 'u').replace('ü', 'u')
                .replace('ç', 'c')
                .trim();
    }

    /** Résultat d'analyse : spécialité + score. */
    public static class ScoredSpecialite {
        public final SpecialiteEntry specialite;
        public final int score;

        public ScoredSpecialite(SpecialiteEntry s, int score) {
            this.specialite = s;
            this.score = score;
        }
    }
}

