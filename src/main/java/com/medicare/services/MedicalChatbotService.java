package com.medicare.services;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.medicare.models.Produit;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java inference of the medical intent classifier trained in
 * chatbot/scripts/train.py and exported to medical_classifier.json.
 *
 * Pipeline (must mirror the training pipeline exactly):
 *   1. tokenize    : regex [a-zA-ZÀ-ſ]+, lowercase
 *   2. ngrams      : unigrams + bigrams
 *   3. vectorize   : count → 1 + log(count) (sublinear_tf) → multiply by IDF
 *   4. normalize   : L2
 *   5. score       : weights · vector + intercept   per class
 *   6. argmax      : pick the class with the highest score
 *   7. softmax     : convert scores to confidence
 */
public class MedicalChatbotService {

    private static final Pattern TOKEN_RE = Pattern.compile("[a-zA-ZÀ-ſ]+", Pattern.UNICODE_CHARACTER_CLASS);

    public static class IntentMeta {
        public String response;
        public List<String> product_types;
        public String urgency;
    }

    public static class Prediction {
        public final String intent;
        public final double confidence;
        public final String response;
        public final List<String> productTypes;
        public final String urgency;
        public final List<Produit> recommendedProducts;

        public Prediction(String intent, double confidence, String response,
                          List<String> productTypes, String urgency, List<Produit> recommendedProducts) {
            this.intent = intent;
            this.confidence = confidence;
            this.response = response;
            this.productTypes = productTypes;
            this.urgency = urgency;
            this.recommendedProducts = recommendedProducts;
        }
    }

    private final ProduitService produitService;

    private Map<String, Integer> vocab;     // ngram → index
    private double[] idf;                    // [n_features]
    private String[] classes;                // [n_classes]
    private double[][] weights;              // [n_classes][n_features]
    private double[] intercepts;             // [n_classes]
    private Map<String, IntentMeta> meta;    // intent_tag → meta
    private boolean sublinearTf;
    private int nMin;
    private int nMax;

    private double accuracy;
    private int nFeatures;
    private boolean ready;

    public MedicalChatbotService() {
        this(new ProduitService());
    }

    public MedicalChatbotService(ProduitService produitService) {
        this.produitService = produitService;
        try {
            loadModel();
            this.ready = true;
            System.out.println("[chatbot] model loaded: "
                    + classes.length + " classes, " + nFeatures + " features, "
                    + "test accuracy " + accuracy);
        } catch (Exception e) {
            this.ready = false;
            System.out.println("[chatbot] FAILED to load model: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isReady() { return ready; }
    public double getAccuracy() { return accuracy; }
    public int getClassCount() { return classes != null ? classes.length : 0; }
    public int getFeatureCount() { return nFeatures; }

    @SuppressWarnings("unchecked")
    private void loadModel() throws Exception {
        try (InputStream is = MedicalChatbotService.class.getResourceAsStream("/medical_classifier.json")) {
            if (is == null) throw new IllegalStateException("medical_classifier.json not found in classpath");
            JsonObject root = new Gson().fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);

            this.sublinearTf = root.get("sublinear_tf").getAsBoolean();
            JsonObject ngrams = root.getAsJsonObject("ngrams");
            this.nMin = ngrams.get("min").getAsInt();
            this.nMax = ngrams.get("max").getAsInt();

            // vocab
            this.vocab = new HashMap<>();
            for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("vocab").entrySet()) {
                vocab.put(e.getKey(), e.getValue().getAsInt());
            }
            this.nFeatures = vocab.size();

            // idf
            this.idf = toDoubleArray(root.getAsJsonArray("idf"));

            // classes
            int nClasses = root.getAsJsonArray("classes").size();
            this.classes = new String[nClasses];
            for (int i = 0; i < nClasses; i++) classes[i] = root.getAsJsonArray("classes").get(i).getAsString();

            // weights matrix
            this.weights = new double[nClasses][];
            for (int i = 0; i < nClasses; i++) {
                weights[i] = toDoubleArray(root.getAsJsonArray("weights").get(i).getAsJsonArray());
            }

            this.intercepts = toDoubleArray(root.getAsJsonArray("intercept"));

            // meta
            Type metaType = new TypeToken<Map<String, IntentMeta>>(){}.getType();
            this.meta = new Gson().fromJson(root.getAsJsonObject("intents_meta"), metaType);

            // metrics
            this.accuracy = root.getAsJsonObject("metrics").get("accuracy").getAsDouble();
        }
    }

    private static double[] toDoubleArray(com.google.gson.JsonArray arr) {
        double[] out = new double[arr.size()];
        for (int i = 0; i < arr.size(); i++) out[i] = arr.get(i).getAsDouble();
        return out;
    }

    // ============================================================
    //  Tokenization & vectorization
    // ============================================================

    private List<String> tokenize(String text) {
        if (text == null) return List.of();
        // The training dataset uses ASCII French (no accents): "tete" not "tête",
        // "a" not "à". Strip accents at inference time so user input matches.
        String src = stripAccentsLower(text);
        List<String> out = new ArrayList<>();
        Matcher m = TOKEN_RE.matcher(src);
        while (m.find()) out.add(m.group());
        return out;
    }

    /** Build count map: ngram → tf */
    private Map<String, Integer> buildCounts(List<String> tokens) {
        Map<String, Integer> counts = new HashMap<>();
        // Unigrams
        if (nMin <= 1 && nMax >= 1) {
            for (String t : tokens) counts.merge(t, 1, Integer::sum);
        }
        // Bigrams
        if (nMin <= 2 && nMax >= 2) {
            for (int i = 0; i + 1 < tokens.size(); i++) {
                counts.merge(tokens.get(i) + " " + tokens.get(i + 1), 1, Integer::sum);
            }
        }
        return counts;
    }

    /** Sparse representation: index → tf-idf value, then L2-normalized. */
    private Map<Integer, Double> vectorize(String text) {
        List<String> tokens = tokenize(text);
        Map<String, Integer> counts = buildCounts(tokens);

        Map<Integer, Double> sparse = new HashMap<>();
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            Integer idx = vocab.get(e.getKey());
            if (idx == null) continue;
            double tf = sublinearTf ? 1.0 + Math.log(e.getValue()) : e.getValue();
            sparse.put(idx, tf * idf[idx]);
        }

        // L2 norm
        double sumSq = 0;
        for (double v : sparse.values()) sumSq += v * v;
        double norm = Math.sqrt(sumSq);
        if (norm > 0) {
            for (Map.Entry<Integer, Double> e : sparse.entrySet()) {
                e.setValue(e.getValue() / norm);
            }
        }
        return sparse;
    }

    // ============================================================
    //  Prediction
    // ============================================================

    public Prediction predict(String userText) {
        if (!ready) {
            return new Prediction("fallback", 0.0,
                    "Le modele n'est pas charge. Reessaie plus tard.",
                    List.of(), "low", List.of());
        }
        Map<Integer, Double> vec = vectorize(userText);

        // Score each class: dot(weights[i], vec) + intercept[i]
        double[] scores = new double[classes.length];
        for (int c = 0; c < classes.length; c++) {
            double s = intercepts[c];
            double[] w = weights[c];
            for (Map.Entry<Integer, Double> e : vec.entrySet()) {
                s += w[e.getKey()] * e.getValue();
            }
            scores[c] = s;
        }

        // Softmax → confidence
        double max = scores[0];
        for (double s : scores) if (s > max) max = s;
        double sumExp = 0;
        double[] probs = new double[classes.length];
        for (int c = 0; c < classes.length; c++) {
            probs[c] = Math.exp(scores[c] - max);
            sumExp += probs[c];
        }
        for (int c = 0; c < classes.length; c++) probs[c] /= sumExp;

        // Argmax
        int best = 0;
        for (int c = 1; c < classes.length; c++) if (probs[c] > probs[best]) best = c;

        String intent = classes[best];
        double confidence = probs[best];

        // If confidence too low → fallback
        if (confidence < 0.20 && !"fallback".equals(intent)) {
            intent = "fallback";
            IntentMeta fbk = meta != null ? meta.get("fallback") : null;
            return new Prediction(intent, confidence,
                    fbk != null ? fbk.response :
                            "Je n'ai pas bien compris. Peux-tu reformuler en decrivant tes symptomes ?",
                    List.of(), "low", List.of());
        }

        IntentMeta m = meta != null ? meta.get(intent) : null;
        String response = m != null ? m.response : "Je suis l'assistant sante Medicare.";
        List<String> productTypes = m != null && m.product_types != null ? m.product_types : List.of();
        String urgency = m != null ? m.urgency : "low";

        // Look up matching products from the catalogue
        List<Produit> recos = recommend(productTypes);
        return new Prediction(intent, confidence, response, productTypes, urgency, recos);
    }

    /** Find products in the catalogue whose `type` matches one of the requested types. */
    private List<Produit> recommend(List<String> types) {
        if (types == null || types.isEmpty()) return List.of();
        List<Produit> out = new ArrayList<>();
        for (String t : types) {
            String needle = stripAccentsLower(t);
            for (Produit p : produitService.getActive()) {
                if (!p.isActive() || p.getQuantity() <= 0) continue;
                String hay = stripAccentsLower(p.getType()) + " " + stripAccentsLower(p.getName())
                        + " " + stripAccentsLower(p.getDescription());
                if (hay.contains(needle.replace('_', ' '))
                        || hay.contains(needle.replace('_', '-'))
                        || hay.contains(needle)) {
                    if (!out.contains(p)) out.add(p);
                    if (out.size() >= 4) return out;
                }
            }
            if (out.size() >= 4) break;
        }
        return out;
    }

    private static String stripAccentsLower(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return n.toLowerCase(Locale.ROOT);
    }
}
