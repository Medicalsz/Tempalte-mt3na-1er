# Training Report — Medical Intent Classifier

- **Total examples**: 7781
- **Train**: 6613 | **Test**: 1168
- **Classes**: 50
- **Vocabulary**: 1725 ngrams (uni + bi)
- **Architecture**: TF-IDF + Logistic Regression (one-vs-rest)

## Accuracy

**0.9940** on held-out test set

## Per-class metrics (test)

| Intent | Precision | Recall | F1 | Support |
|---|---|---|---|---|
| acne | 1.00 | 1.00 | 1.00 | 30 |
| allaitement_question | 1.00 | 0.67 | 0.80 | 3 |
| allergie | 1.00 | 1.00 | 1.00 | 30 |
| anxiete | 1.00 | 1.00 | 1.00 | 30 |
| asthme | 1.00 | 1.00 | 1.00 | 30 |
| au_revoir | 0.00 | 0.00 | 0.00 | 1 |
| brulure | 1.00 | 1.00 | 1.00 | 30 |
| brulure_estomac | 1.00 | 1.00 | 1.00 | 30 |
| constipation | 1.00 | 1.00 | 1.00 | 30 |
| contraception | 1.00 | 1.00 | 1.00 | 30 |
| controle_poids | 0.00 | 0.00 | 0.00 | 1 |
| coupure_legere | 1.00 | 1.00 | 1.00 | 30 |
| diabete_info | 1.00 | 1.00 | 1.00 | 30 |
| diarrhee | 1.00 | 1.00 | 1.00 | 30 |
| douleur_abdominale | 1.00 | 1.00 | 1.00 | 30 |
| douleur_articulaire | 1.00 | 1.00 | 1.00 | 30 |
| douleur_musculaire | 1.00 | 1.00 | 1.00 | 30 |
| douleur_oculaire | 1.00 | 1.00 | 1.00 | 30 |
| douleur_oreille | 1.00 | 1.00 | 1.00 | 30 |
| effets_secondaires | 1.00 | 1.00 | 1.00 | 30 |
| epistaxis | 1.00 | 1.00 | 1.00 | 30 |
| fallback | 0.00 | 0.00 | 0.00 | 1 |
| fatigue | 1.00 | 1.00 | 1.00 | 30 |
| fievre | 0.97 | 1.00 | 0.98 | 30 |
| grossesse_question | 0.97 | 1.00 | 0.98 | 30 |
| hemorroides | 0.97 | 1.00 | 0.98 | 30 |
| hypertension_info | 1.00 | 1.00 | 1.00 | 30 |
| hypoglycemie | 1.00 | 1.00 | 1.00 | 30 |
| info_dosage | 1.00 | 1.00 | 1.00 | 1 |
| info_produit | 1.00 | 1.00 | 1.00 | 11 |
| insomnie | 0.97 | 1.00 | 0.98 | 30 |
| interactions_medicaments | 1.00 | 1.00 | 1.00 | 3 |
| mal_de_gorge | 1.00 | 1.00 | 1.00 | 30 |
| mal_de_tete | 1.00 | 1.00 | 1.00 | 30 |
| mal_dents | 1.00 | 1.00 | 1.00 | 30 |
| mycose | 1.00 | 1.00 | 1.00 | 30 |
| nausees | 0.97 | 1.00 | 0.98 | 30 |
| passer_commande | 0.67 | 1.00 | 0.80 | 2 |
| pediatrie_dosage | 0.00 | 0.00 | 0.00 | 1 |
| piqure_insecte | 1.00 | 1.00 | 1.00 | 30 |
| regles_douloureuses | 1.00 | 1.00 | 1.00 | 30 |
| remerciement | 1.00 | 1.00 | 1.00 | 1 |
| rhume | 1.00 | 1.00 | 1.00 | 30 |
| salutation | 0.00 | 0.00 | 0.00 | 1 |
| toux_grasse | 1.00 | 1.00 | 1.00 | 30 |
| toux_seche | 1.00 | 1.00 | 1.00 | 30 |
| urgence | 0.97 | 1.00 | 0.98 | 30 |
| vaccin_info | 1.00 | 0.50 | 0.67 | 2 |
| vertiges | 1.00 | 1.00 | 1.00 | 30 |
| vue_question | 1.00 | 1.00 | 1.00 | 30 |

## Notes
- Tokenizer : regex `[a-zA-ZÀ-ſ]+` (lowercased)
- N-grams : 1+2
- `sublinear_tf=True` (TF replaced by 1+log(TF))
- `norm=l2` (L2 row normalisation)
- Solver : liblinear (one-vs-rest)
