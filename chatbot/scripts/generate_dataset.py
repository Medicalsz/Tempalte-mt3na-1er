"""
Combinatorial dataset generator for the Medicare medical chatbot.

Builds ~10,000 French-language phrasings across ~50 medical intents
by combining template parts (subjects, verbs, symptoms, modifiers).
Output: chatbot/data/medical_intents.json + chatbot/data/training_set.json
"""
import json
import random
import os
import itertools
from pathlib import Path

random.seed(42)

OUT_DIR = Path(__file__).resolve().parent.parent / "data"
OUT_DIR.mkdir(parents=True, exist_ok=True)

# Common phrasing parts reused across intents
SUBJECTS = [
    "j'ai", "je sens", "je ressens", "je souffre de", "il y a",
    "depuis ce matin j'ai", "depuis hier j'ai", "depuis quelques jours j'ai",
    "ce matin j'avais", "soudain j'ai", "j'ai eu", "ma femme a",
    "mon enfant a", "mon mari a", "mon pere a", "ma mere a",
    "depuis ce matin je sens", "j'arrive pas a dormir avec", "ca fait mal",
]

INTENSITIES = ["", "un peu", "tres", "beaucoup", "vraiment", "fort", "leger", "modere", "intense", "supportable", "insupportable"]
DURATIONS = ["", "depuis ce matin", "depuis hier", "depuis 2 jours", "depuis une semaine", "depuis quelques heures", "d'un coup", "sans arret", "qui revient", "qui s'aggrave"]


def expand(intent_tag, response, products, urgency, templates, n=200, prefixes=None):
    """Combinatorial expansion of templates into ~n phrasings."""
    if prefixes is None:
        prefixes = SUBJECTS
    out = set()
    pool = []
    for tpl in templates:
        for sub in prefixes:
            for inten in INTENSITIES:
                for dur in DURATIONS:
                    txt = tpl.replace("{S}", sub).replace("{I}", inten).replace("{D}", dur)
                    txt = " ".join(txt.split())  # collapse spaces
                    pool.append(txt.strip())
    random.shuffle(pool)
    for txt in pool:
        if len(out) >= n:
            break
        out.add(txt)
    return {
        "tag": intent_tag,
        "response": response,
        "product_types": products,
        "urgency": urgency,
        "examples": sorted(out),
    }


# ---------------- INTENT DEFINITIONS ----------------
INTENTS = []

# 1. Mal de tete
INTENTS.append(expand(
    "mal_de_tete",
    "Pour un mal de tete, un antalgique simple peut soulager. Si la douleur dure plus de 48h ou s'aggrave, consulte un medecin.",
    ["antalgique", "anti-inflammatoire"],
    "low",
    [
        "{S} mal a la tete {I} {D}",
        "{S} une migraine {I} {D}",
        "{S} des cephalees {I} {D}",
        "{S} la tete qui tape {D}",
        "{S} les tempes qui me font mal {D}",
        "{S} un mal de crane {I} {D}",
    ],
))

# 2. Fievre
INTENTS.append(expand(
    "fievre",
    "Pour de la fievre, un antipyretique aide. Si la temperature depasse 39C ou dure plus de 3 jours, consulte rapidement.",
    ["antipyretique", "antalgique"],
    "medium",
    [
        "{S} de la fievre {I} {D}",
        "{S} 38 de fievre {D}",
        "{S} 39 de fievre {D}",
        "{S} 40 de fievre {D}",
        "{S} de la temperature {I} {D}",
        "{S} chaud {D}",
        "je suis brulant {D}",
        "{S} des frissons et de la fievre {D}",
    ],
))

# 3. Toux
INTENTS.append(expand(
    "toux_seche",
    "Pour une toux seche, un sirop antitussif peut aider. Si elle persiste plus d'une semaine ou s'accompagne de fievre, consulte.",
    ["antitussif", "sirop"],
    "low",
    [
        "{S} une toux seche {I} {D}",
        "{S} la toux {I} {D}",
        "{S} qui tousse {D}",
        "{S} une toux qui ne part pas {D}",
        "je tousse beaucoup {D}",
        "ma toux est {I} {D}",
        "{S} qui tousse sans cracher {D}",
    ],
))

# 4. Toux grasse
INTENTS.append(expand(
    "toux_grasse",
    "Pour une toux grasse avec mucus, un sirop expectorant aide a evacuer. Bois beaucoup d'eau et repose-toi.",
    ["expectorant", "sirop"],
    "low",
    [
        "{S} une toux grasse {D}",
        "{S} qui tousse avec du mucus {D}",
        "{S} qui crache en toussant {D}",
        "{S} des glaires {D}",
        "{S} une toux productive {D}",
    ],
))

# 5. Mal de gorge
INTENTS.append(expand(
    "mal_de_gorge",
    "Pour un mal de gorge, des pastilles antiseptiques et des boissons chaudes soulagent. Si fievre + ganglions, consulte.",
    ["antiseptique_gorge", "pastilles", "antalgique"],
    "low",
    [
        "{S} mal a la gorge {I} {D}",
        "{S} la gorge qui pique {D}",
        "{S} la gorge qui brule {D}",
        "{S} du mal a avaler {D}",
        "{S} une angine {D}",
        "{S} la gorge enflammee {D}",
    ],
))

# 6. Rhume
INTENTS.append(expand(
    "rhume",
    "Pour un rhume, un decongestionnant nasal et du repos suffisent generalement. Hydratation +++.",
    ["decongestionnant", "antihistaminique"],
    "low",
    [
        "{S} un rhume {D}",
        "{S} le nez qui coule {D}",
        "{S} le nez bouche {D}",
        "{S} attrape un rhume {D}",
        "{S} un nez qui coule sans arret {D}",
        "{S} eternue {D}",
    ],
))

# 7. Allergie
INTENTS.append(expand(
    "allergie",
    "Pour une reaction allergique legere, un antihistaminique peut aider. Si gonflement, difficulte a respirer ou eruption etendue : urgences.",
    ["antihistaminique"],
    "medium",
    [
        "{S} une reaction allergique {D}",
        "{S} des plaques rouges qui demangent {D}",
        "{S} de l'urticaire {D}",
        "{S} les yeux qui pleurent et qui demangent {D}",
        "{S} une rhinite allergique {D}",
        "{S} les yeux gonflees {D}",
    ],
))

# 8. Douleur abdominale
INTENTS.append(expand(
    "douleur_abdominale",
    "Pour des douleurs au ventre, un antispasmodique peut soulager. Si la douleur est intense, persistante ou avec fievre, consulte rapidement.",
    ["antispasmodique", "antalgique"],
    "medium",
    [
        "{S} mal au ventre {I} {D}",
        "{S} des crampes au ventre {D}",
        "{S} l'estomac qui me fait mal {D}",
        "{S} mal au bas-ventre {D}",
        "{S} des douleurs abdominales {D}",
        "{S} l'abdomen qui me lance {D}",
    ],
))

# 9. Brulure d'estomac
INTENTS.append(expand(
    "brulure_estomac",
    "Pour des brulures d'estomac, un antiacide peut aider. Evite les aliments epices, le cafe et l'alcool.",
    ["antiacide", "ipp"],
    "low",
    [
        "{S} des brulures d'estomac {D}",
        "{S} des remontees acides {D}",
        "{S} l'estomac qui brule {D}",
        "{S} du reflux {D}",
        "{S} la pyrosis {D}",
        "{S} aigreurs {D}",
    ],
))

# 10. Nausees / vomissements
INTENTS.append(expand(
    "nausees",
    "Pour des nausees, un antiemetique peut aider. Hydratation par petites gorgees. Si vomissements repetes, consulte.",
    ["antiemetique"],
    "medium",
    [
        "{S} envie de vomir {D}",
        "{S} des nausees {D}",
        "{S} mal au coeur {D}",
        "{S} vomi {D}",
        "{S} l'envie de vomir {I} {D}",
    ],
))

# 11. Diarrhee
INTENTS.append(expand(
    "diarrhee",
    "Pour la diarrhee, hydratation +++ et un antidiarrheique peuvent aider. Si sang, fievre ou plus de 3 jours, consulte.",
    ["antidiarrheique", "rehydratant"],
    "medium",
    [
        "{S} la diarrhee {D}",
        "{S} des selles liquides {D}",
        "{S} mal au ventre avec diarrhee {D}",
        "{S} la chiasse {D}",
        "{S} aux toilettes sans arret {D}",
    ],
))

# 12. Constipation
INTENTS.append(expand(
    "constipation",
    "Pour la constipation, un laxatif doux peut aider. Augmente l'hydratation, les fibres et l'activite physique.",
    ["laxatif"],
    "low",
    [
        "{S} constipe {D}",
        "{S} pas alle aux toilettes {D}",
        "{S} du mal a aller a la selle {D}",
        "{S} de la constipation {D}",
    ],
))

# 13. Insomnie
INTENTS.append(expand(
    "insomnie",
    "Pour les troubles du sommeil legers, des plantes (valeriane, melatonine) peuvent aider. Reduis les ecrans avant de dormir.",
    ["somnifere", "melatonine"],
    "low",
    [
        "{S} du mal a dormir {D}",
        "{S} des insomnies {D}",
        "{S} pas dormi {D}",
        "{S} qui n'arrive pas a dormir {D}",
        "{S} reveille la nuit {D}",
        "j'ai du mal a m'endormir {D}",
    ],
))

# 14. Anxiete legere
INTENTS.append(expand(
    "anxiete",
    "Pour l'anxiete legere, des plantes apaisantes (passiflore, valeriane) peuvent aider. Si les symptomes persistent, consulte un medecin.",
    ["anxiolytique_plante"],
    "medium",
    [
        "{S} stresse {D}",
        "{S} anxieux {D}",
        "{S} angoisse {D}",
        "{S} le coeur qui bat fort de stress {D}",
        "{S} de l'anxiete {D}",
    ],
))

# 15. Douleur musculaire
INTENTS.append(expand(
    "douleur_musculaire",
    "Pour des courbatures, un anti-inflammatoire local ou oral aide. Repos et chaleur aussi.",
    ["anti-inflammatoire", "myorelaxant", "creme_chauffante"],
    "low",
    [
        "{S} mal au dos {D}",
        "{S} mal aux muscles {D}",
        "{S} des courbatures {D}",
        "{S} le cou raide {D}",
        "{S} l'epaule qui me fait mal {D}",
        "{S} mal aux jambes {D}",
        "{S} des douleurs musculaires {D}",
    ],
))

# 16. Douleur articulaire
INTENTS.append(expand(
    "douleur_articulaire",
    "Pour des douleurs articulaires, un anti-inflammatoire aide. Si gonflement, rougeur ou fievre, consulte.",
    ["anti-inflammatoire", "antalgique"],
    "medium",
    [
        "{S} mal aux articulations {D}",
        "{S} le genou qui me fait mal {D}",
        "{S} mal au poignet {D}",
        "{S} mal au coude {D}",
        "{S} les doigts qui me font mal {D}",
        "{S} de l'arthrose {D}",
    ],
))

# 17. Coupure / petite plaie
INTENTS.append(expand(
    "coupure_legere",
    "Pour une petite coupure : laver a l'eau et savon, desinfecter, pansement. Si plaie profonde ou souillee : urgences.",
    ["antiseptique", "pansement"],
    "low",
    [
        "{S} coupe au doigt {D}",
        "{S} une petite plaie {D}",
        "{S} fait une coupure {D}",
        "{S} egratigne {D}",
        "{S} m'estsblesse legerement {D}",
    ],
))

# 18. Brulure
INTENTS.append(expand(
    "brulure",
    "Pour une brulure superficielle : eau froide 15 min puis creme cicatrisante. Si etendue, profonde ou chez un enfant : urgences.",
    ["pommade_brulure", "antiseptique"],
    "medium",
    [
        "{S} fait une brulure {D}",
        "{S} brule la main {D}",
        "{S} brule en cuisinant {D}",
        "{S} une brulure superficielle {D}",
        "{S} pris un coup de soleil {D}",
    ],
))

# 19. Piqure d'insecte
INTENTS.append(expand(
    "piqure_insecte",
    "Pour une piqure : laver, glacer, creme apaisante ou antihistaminique local. Si gonflement diffus ou difficulte respiratoire : urgences.",
    ["antihistaminique", "creme_apaisante"],
    "low",
    [
        "{S} pique par un moustique {D}",
        "{S} une piqure d'abeille {D}",
        "{S} une piqure de guepe {D}",
        "{S} pique par une fourmi {D}",
        "{S} des piqures qui me grattent {D}",
    ],
))

# 20. Mal aux yeux
INTENTS.append(expand(
    "douleur_oculaire",
    "Pour irritation oculaire, des larmes artificielles ou un collyre apaisant aident. Si douleur intense, vision trouble ou rougeur persistante, consulte un ophtalmo.",
    ["collyre", "larmes_artificielles"],
    "low",
    [
        "{S} les yeux qui piquent {D}",
        "{S} les yeux secs {D}",
        "{S} les yeux rouges {D}",
        "{S} mal aux yeux {D}",
        "{S} les yeux fatigues {D}",
        "{S} de la conjonctivite {D}",
    ],
))

# 21. Boutons / acne
INTENTS.append(expand(
    "acne",
    "Pour l'acne legere, une creme adaptee + nettoyage doux. Si severe, dermato.",
    ["creme_acne"],
    "low",
    [
        "{S} des boutons {D}",
        "{S} de l'acne {D}",
        "{S} le visage qui pousse {D}",
        "{S} des points noirs {D}",
        "{S} la peau grasse {D}",
    ],
))

# 22. Mycose
INTENTS.append(expand(
    "mycose",
    "Pour une mycose, une creme antifongique aide. Garder la zone propre et seche.",
    ["antifongique"],
    "low",
    [
        "{S} une mycose {D}",
        "{S} le pied d'athlete {D}",
        "{S} entre les orteils qui pelle {D}",
        "{S} une mycose vaginale {D}",
        "{S} les ongles qui jaunissent {D}",
    ],
))

# 23. Hemorroides
INTENTS.append(expand(
    "hemorroides",
    "Pour les hemorroides, une creme apaisante et un regime riche en fibres aident. Si saignements importants, consulte.",
    ["creme_hemorroides", "veinotonique"],
    "low",
    [
        "{S} des hemorroides {D}",
        "{S} mal a l'anus {D}",
        "{S} des saignements aux toilettes {D}",
        "{S} ca brule en allant a la selle {D}",
    ],
))

# 24. Cycle / regles douloureuses
INTENTS.append(expand(
    "regles_douloureuses",
    "Pour des douleurs de regles, un antalgique ou antispasmodique aide. Bouillotte chaude aussi.",
    ["antalgique", "antispasmodique"],
    "low",
    [
        "{S} mal aux regles {D}",
        "{S} des regles douloureuses {D}",
        "{S} des crampes menstruelles {D}",
        "{S} mal au bas-ventre pendant mes regles {D}",
    ],
))

# 25. Mal de dents
INTENTS.append(expand(
    "mal_dents",
    "Pour un mal de dents, un antalgique calme la douleur en attendant le dentiste. Consulte rapidement, ne traine pas.",
    ["antalgique", "anti-inflammatoire"],
    "medium",
    [
        "{S} mal aux dents {D}",
        "{S} la dent qui pulse {D}",
        "{S} une rage de dents {D}",
        "{S} mal a la machoire {D}",
        "{S} un abces dentaire {D}",
    ],
))

# 26. Otite / mal d'oreille
INTENTS.append(expand(
    "douleur_oreille",
    "Pour un mal d'oreille, un antalgique soulage. Si fievre, ecoulement ou enfant : consulte un medecin pour eviter une complication.",
    ["antalgique", "anti-inflammatoire"],
    "medium",
    [
        "{S} mal a l'oreille {D}",
        "{S} l'oreille bouchee {D}",
        "{S} une otite {D}",
        "{S} l'oreille qui me fait mal {D}",
    ],
))

# 27. Vertige
INTENTS.append(expand(
    "vertiges",
    "Les vertiges peuvent avoir plusieurs causes. Repose-toi assis. Si repetes ou avec autres symptomes, consulte.",
    ["antivertigineux"],
    "medium",
    [
        "{S} des vertiges {D}",
        "{S} la tete qui tourne {D}",
        "{S} l'impression que tout tourne {D}",
        "{S} un malaise {D}",
        "{S} la tete qui tourne quand je me leve {D}",
    ],
))

# 28. Hypertension
INTENTS.append(expand(
    "hypertension_info",
    "L'hypertension necessite un suivi medical regulier. Reduis le sel, l'alcool, fais du sport. Pour un traitement, consulte ton medecin.",
    ["antihypertenseur"],
    "high",
    [
        "{S} de la tension {D}",
        "{S} hypertendu {D}",
        "{S} la tension qui monte {D}",
        "{S} 16 de tension {D}",
    ],
))

# 29. Diabete info
INTENTS.append(expand(
    "diabete_info",
    "Le diabete necessite un suivi medical. Surveille ton alimentation et tes glycemies. Consulte ton medecin pour le traitement.",
    ["antidiabetique"],
    "high",
    [
        "{S} du diabete {D}",
        "{S} diabetique {D}",
        "{S} le sucre eleve {D}",
        "{S} la glycemie haute {D}",
    ],
))

# 30. Asthme
INTENTS.append(expand(
    "asthme",
    "Pour une crise d'asthme legere, utilise ton ventoline. Si la respiration ne s'ameliore pas en 5-10 min, urgences.",
    ["bronchodilatateur"],
    "high",
    [
        "{S} du mal a respirer {D}",
        "{S} de l'asthme {D}",
        "{S} une crise d'asthme {D}",
        "{S} la respiration sifflante {D}",
        "{S} essouffle {D}",
    ],
))

# 31. Hypoglycemie
INTENTS.append(expand(
    "hypoglycemie",
    "Pour une hypoglycemie : sucre rapide (jus, bonbon) + 15 min de repos. Si pertes de connaissance frequentes, consulte rapidement.",
    ["glucose"],
    "high",
    [
        "{S} l'hypoglycemie {D}",
        "{S} le sucre bas {D}",
        "{S} faiblesse de faim {D}",
        "{S} les mains qui tremblent de faim {D}",
    ],
))

# 32. Saignement de nez
INTENTS.append(expand(
    "epistaxis",
    "Pour un saignement de nez : pencher la tete en avant, comprimer 10 min. Si recurrent ou abondant, consulte.",
    ["coagulant_local"],
    "low",
    [
        "{S} le nez qui saigne {D}",
        "{S} un saignement de nez {D}",
        "{S} de l'epistaxis {D}",
    ],
))

# 33. Fatigue
INTENTS.append(expand(
    "fatigue",
    "Pour de la fatigue passagere, des vitamines (B, C, D, magnesium) peuvent aider. Si persistante > 2 semaines, consulte.",
    ["vitamines", "magnesium"],
    "low",
    [
        "{S} fatigue {D}",
        "{S} epuise {D}",
        "{S} pas d'energie {D}",
        "{S} crevee {D}",
        "{S} sans force {D}",
    ],
))

# 34. Poids - perdre
INTENTS.append(expand(
    "controle_poids",
    "Pour la perte de poids, l'essentiel reste alimentation + activite physique. Pour des conseils personnalises, consulte un nutritionniste.",
    ["complement_alimentaire"],
    "low",
    [
        "{S} envie de perdre du poids",
        "{S} grossi",
        "je veux maigrir",
        "comment perdre du ventre",
    ],
    prefixes=[""],
))

# 35. Question dosage
INTENTS.append(expand(
    "info_dosage",
    "Pour le dosage exact, lis la notice du medicament et respecte les indications. En cas de doute, demande au pharmacien.",
    [],
    "low",
    [
        "combien de doliprane je peux prendre",
        "quel dosage pour {S}",
        "combien de gouttes",
        "combien de fois par jour",
        "quelle est la posologie",
    ],
    prefixes=["", "moi", "un adulte", "un enfant"],
))

# 36. Question grossesse
INTENTS.append(expand(
    "grossesse_question",
    "Pendant la grossesse, certains medicaments sont a eviter. Demande systematiquement a ton medecin ou pharmacien avant de prendre quoi que ce soit.",
    [],
    "high",
    [
        "{S} enceinte est-ce que je peux prendre",
        "{S} enceinte je peux prendre {D}",
        "{S} enceinte j'ai mal a la tete",
    ],
))

# 37. Question allaitement
INTENTS.append(expand(
    "allaitement_question",
    "Pendant l'allaitement, certains medicaments passent dans le lait. Demande au pharmacien ou medecin avant.",
    [],
    "medium",
    [
        "j'allaite est-ce que je peux prendre",
        "j'allaite et j'ai mal a la tete",
        "{S} qui allaite et veut prendre {D}",
    ],
    prefixes=["", "ma femme"],
))

# 38. Question enfant
INTENTS.append(expand(
    "pediatrie_dosage",
    "Pour les enfants, le dosage depend du poids et de l'age. Verifie la notice et n'hesite pas a demander au pharmacien.",
    [],
    "medium",
    [
        "mon enfant a mal a la tete",
        "mon bebe pleure",
        "ma fille de 5 ans a de la fievre",
        "mon fils a 7 ans peut-il prendre",
    ],
    prefixes=[""],
))

# 39. Effets secondaires
INTENTS.append(expand(
    "effets_secondaires",
    "Si tu ressens des effets secondaires, arrete le medicament et contacte ton medecin ou pharmacien rapidement.",
    [],
    "medium",
    [
        "{S} des effets secondaires depuis que je prends {D}",
        "{S} des nausees apres avoir pris {D}",
        "{S} mal au ventre depuis le medicament {D}",
        "{S} une reaction au medicament {D}",
    ],
))

# 40. Interactions
INTENTS.append(expand(
    "interactions_medicaments",
    "Les interactions entre medicaments peuvent etre serieuses. Liste tous tes medicaments au pharmacien avant d'en prendre un nouveau.",
    [],
    "high",
    [
        "est-ce que je peux prendre X avec Y",
        "{S} deja un medicament je peux prendre {D}",
        "y a-t-il une interaction entre",
        "compatible avec",
    ],
    prefixes=["", "je prends deja"],
))

# 41. Vaccin
INTENTS.append(expand(
    "vaccin_info",
    "Pour les vaccins, consulte ton medecin ou un centre de vaccination. La pharmacie peut delivrer certains vaccins prescrits.",
    [],
    "low",
    [
        "vaccin contre la grippe",
        "vaccin tetanos",
        "rappel de vaccin",
        "ou se faire vacciner",
        "le vaccin {D}",
    ],
    prefixes=[""],
))

# 42. Contraception
INTENTS.append(expand(
    "contraception",
    "Pour la contraception, consulte un gyneco ou un medecin generaliste. La pilule du lendemain est en vente libre en pharmacie.",
    [],
    "medium",
    [
        "{S} besoin de la pilule du lendemain",
        "contraception d'urgence",
        "pilule contraceptive {D}",
        "{S} oublie ma pilule {D}",
    ],
))

# 43. Vue / lunettes
INTENTS.append(expand(
    "vue_question",
    "Pour des problemes de vue, consulte un ophtalmologiste. Pour des lunettes, un opticien.",
    [],
    "low",
    [
        "{S} la vue qui baisse {D}",
        "{S} du mal a voir de loin {D}",
        "{S} besoin de lunettes",
        "{S} les yeux fatigues a cause de l'ecran {D}",
    ],
))

# 44. Salutation / start
INTENTS.append(expand(
    "salutation",
    "Bonjour ! Je suis l'assistant sante Medicare. Je peux te conseiller sur des symptomes courants. Decris ce que tu ressens.",
    [],
    "low",
    [
        "bonjour",
        "salut",
        "hey",
        "hello",
        "coucou",
        "bonsoir",
        "bonjour docteur",
    ],
    prefixes=[""],
))

# 45. Remerciement
INTENTS.append(expand(
    "remerciement",
    "Avec plaisir ! N'hesite pas si tu as d'autres questions. Bonne sante !",
    [],
    "low",
    [
        "merci",
        "merci beaucoup",
        "super merci",
        "ok merci",
        "thanks",
        "merci pour l'aide",
    ],
    prefixes=[""],
))

# 46. Au revoir
INTENTS.append(expand(
    "au_revoir",
    "Au revoir ! Prends soin de toi.",
    [],
    "low",
    [
        "au revoir",
        "bye",
        "salut a plus",
        "ciao",
        "a la prochaine",
    ],
    prefixes=[""],
))

# 47. Demande commande
INTENTS.append(expand(
    "passer_commande",
    "Tu peux passer commande directement depuis l'onglet Produits de l'application.",
    [],
    "low",
    [
        "comment commander",
        "je veux passer commande",
        "ou commander {D}",
        "{S} besoin de commander un produit",
    ],
    prefixes=[""],
))

# 48. Urgence vitale
INTENTS.append(expand(
    "urgence",
    "URGENCE : appelle immediatement le 190 (SAMU Tunisie) ou rends-toi aux urgences les plus proches.",
    [],
    "critical",
    [
        "{S} mal au coeur tres fort {D}",
        "{S} une douleur dans la poitrine qui irradie {D}",
        "{S} perdu connaissance {D}",
        "{S} du mal a respirer fortement {D}",
        "je crois que je fais un AVC",
        "je crois que je fais un infarctus",
        "{S} crache du sang {D}",
        "{S} envie de mourir",
    ],
))

# 49. Pas compris
INTENTS.append(expand(
    "fallback",
    "Je n'ai pas bien compris. Peux-tu reformuler en decrivant tes symptomes ou ce que tu cherches ?",
    [],
    "low",
    [
        "azertyuiop",
        "blablabla",
        "je sais pas",
        "n'importe quoi",
        "...",
    ],
    prefixes=[""],
))

# 50. Question sur produit Medicare
INTENTS.append(expand(
    "info_produit",
    "Tu peux consulter le catalogue des produits Medicare dans l'onglet 'Nos produits'. Tu y trouveras la description, le prix et le stock.",
    [],
    "low",
    [
        "vous avez {D}",
        "{S} cherche un medicament pour {D}",
        "{S} besoin d'un produit pour {D}",
        "vous vendez {D}",
        "y a t il du {D} en stock",
    ],
    prefixes=["", "est-ce que"],
))


# ---------------- BUILD DATASET ----------------
total = sum(len(i["examples"]) for i in INTENTS)
print(f"Total examples: {total} | Intents: {len(INTENTS)}")

with open(OUT_DIR / "medical_intents.json", "w", encoding="utf-8") as f:
    json.dump({"intents": INTENTS}, f, ensure_ascii=False, indent=2)

# Flat training set (label, text)
flat = []
for it in INTENTS:
    for ex in it["examples"]:
        flat.append({"label": it["tag"], "text": ex})

random.shuffle(flat)
with open(OUT_DIR / "training_set.json", "w", encoding="utf-8") as f:
    json.dump(flat, f, ensure_ascii=False)

print(f"Flat training set: {len(flat)} examples saved to {OUT_DIR / 'training_set.json'}")
