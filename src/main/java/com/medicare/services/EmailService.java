package com.medicare.services;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class EmailService {

    private static final String SMTP_HOST      = "smtp.gmail.com";
    private static final String SMTP_PORT      = "587";
    private static final String EMAIL_FROM     = "ayoubadjida80@gmail.com";
    private static final String EMAIL_PASSWORD = "uwdptcxvdcpibjvs";

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       "smtp.gmail.com");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(EMAIL_FROM, EMAIL_PASSWORD);
            }
        });
    }

    public void envoyerEmail(String destinataire, String sujet, String contenu) {
        new Thread(() -> {
            try {
                Session session = createSession();
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(EMAIL_FROM, "MediCare"));
                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinataire));
                message.setSubject(sujet);
                message.setContent(contenu, "text/html; charset=utf-8");
                Transport.send(message);
                System.out.println("✅ Email envoyé à : " + destinataire);
            } catch (Exception e) {
                System.err.println("❌ Erreur envoi email : " + e.getMessage());
            }
        }).start();
    }

    // ── Confirmation RDV ──────────────────────────────────────────────────
    public void envoyerConfirmationRdv(String emailPatient, String nomPatient,
                                        String nomMedecin, String date, String heure) {
        String sujet = "✅ Confirmation de votre rendez-vous - MediCare";
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#1976D2,#42A5F5);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">🏥 MediCare</h1>
                <p style="color:#e3f2fd;margin:5px 0 0;">Votre santé, notre priorité</p>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#1976D2;">Bonjour %s,</h2>
                <p style="color:#555;font-size:16px;">Votre rendez-vous a été <strong style="color:#4CAF50;">✅ confirmé</strong> avec succès.</p>
                <div style="background:#E3F2FD;border-left:4px solid #1976D2;padding:20px;margin:20px 0;border-radius:8px;">
                  <p style="margin:8px 0;font-size:15px;">👨‍⚕️ <strong>Médecin :</strong> Dr. %s</p>
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Date :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">🕐 <strong>Heure :</strong> %s</p>
                </div>
                <p style="color:#777;font-size:14px;background:#f9f9f9;padding:12px;border-radius:6px;">
                  💡 Pensez à vous présenter <strong>10 minutes avant</strong> votre rendez-vous.
                </p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomPatient, nomMedecin, date, heure);
        envoyerEmail(emailPatient, sujet, contenu);
    }

    // ── Rappel 24h avant le RDV ───────────────────────────────────────────
    public void envoyerRappelRdv(String emailPatient, String nomPatient,
                                  String nomMedecin, String date, String heure) {
        String sujet = "⏰ Rappel : Votre rendez-vous demain - MediCare";
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#F57C00,#FFB74D);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">⏰ Rappel MediCare</h1>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#F57C00;">Bonjour %s,</h2>
                <p style="color:#555;font-size:16px;">Vous avez un rendez-vous <strong>demain</strong>.</p>
                <div style="background:#FFF8E1;border-left:4px solid #F57C00;padding:20px;margin:20px 0;border-radius:8px;">
                  <p style="margin:8px 0;font-size:15px;">👨‍⚕️ <strong>Médecin :</strong> Dr. %s</p>
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Date :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">🕐 <strong>Heure :</strong> %s</p>
                </div>
                <p style="color:#777;font-size:14px;">N'oubliez pas votre carte vitale et vos documents médicaux.</p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomPatient, nomMedecin, date, heure);
        envoyerEmail(emailPatient, sujet, contenu);
    }

    // ── Annulation RDV ────────────────────────────────────────────────────
    public void envoyerAnnulationRdv(String emailPatient, String nomPatient,
                                      String nomMedecin, String date, String heure, String motif) {
        String sujet = "❌ Annulation de votre rendez-vous - MediCare";
        String motifAffiche = (motif != null && !motif.isEmpty()) ? motif : "Non précisé";
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#c62828,#ef5350);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">❌ Annulation MediCare</h1>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#c62828;">Bonjour %s,</h2>
                <p style="color:#555;font-size:16px;">Votre rendez-vous a été <strong style="color:#c62828;">annulé</strong>.</p>
                <div style="background:#FFEBEE;border-left:4px solid #c62828;padding:20px;margin:20px 0;border-radius:8px;">
                  <p style="margin:8px 0;font-size:15px;">👨‍⚕️ <strong>Médecin :</strong> Dr. %s</p>
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Date :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">🕐 <strong>Heure :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">📝 <strong>Motif :</strong> %s</p>
                </div>
                <p style="color:#555;font-size:14px;">Vous pouvez reprendre un nouveau rendez-vous sur MediCare.</p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomPatient, nomMedecin, date, heure, motifAffiche);
        envoyerEmail(emailPatient, sujet, contenu);
    }

    // ── Report proposé par le médecin ─────────────────────────────────────
    public void envoyerReportRdv(String emailPatient, String nomPatient,
                                  String nomMedecin, String ancienneDate,
                                  String nouvelleDate, String nouvelleHeure) {
        String sujet = "📅 Report de votre rendez-vous - MediCare";
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#6A1B9A,#AB47BC);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">📅 Report MediCare</h1>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#6A1B9A;">Bonjour %s,</h2>
                <p style="color:#555;font-size:16px;">Le Dr. <strong>%s</strong> vous propose de <strong>reporter</strong> votre rendez-vous.</p>
                <div style="background:#F3E5F5;border-left:4px solid #6A1B9A;padding:20px;margin:20px 0;border-radius:8px;">
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Ancienne date :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Nouvelle date proposée :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">🕐 <strong>Nouvelle heure :</strong> %s</p>
                </div>
                <p style="color:#555;font-size:14px;">Connectez-vous sur MediCare pour <strong>accepter ou refuser</strong> ce report.</p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomPatient, nomMedecin, ancienneDate, nouvelleDate, nouvelleHeure);
        envoyerEmail(emailPatient, sujet, contenu);
    }

    // ── Créneau libéré → notifier le 1er en liste d'attente ─────────────
    public void envoyerCreneauLibere(String emailPatient, String nomPatient,
                                      String nomMedecin, String date, String heure) {
        String sujet = "🎉 Un créneau s'est libéré ! - MediCare";
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#16a34a,#4ade80);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">🏥 MediCare</h1>
                <p style="color:#dcfce7;margin:5px 0 0;">Bonne nouvelle !</p>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#16a34a;">Bonjour %s,</h2>
                <p style="color:#555;font-size:16px;">
                  🎉 Un créneau s'est <strong style="color:#16a34a;">libéré</strong> avec le Dr. <strong>%s</strong> !
                </p>
                <div style="background:#dcfce7;border-left:4px solid #16a34a;padding:20px;margin:20px 0;border-radius:8px;">
                  <p style="margin:8px 0;font-size:15px;">👨‍⚕️ <strong>Médecin :</strong> Dr. %s</p>
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Date :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">🕐 <strong>Créneau :</strong> %s</p>
                </div>
                <p style="color:#555;font-size:14px;">
                  Connectez-vous sur <strong>MediCare</strong> rapidement pour prendre ce rendez-vous avant qu'il soit pris par quelqu'un d'autre !
                </p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomPatient, nomMedecin, nomMedecin, date, heure);
        envoyerEmail(emailPatient, sujet, contenu);
    }

    // ── Aucun créneau libéré → date expirée ──────────────────────────────
    public void envoyerAttenteExpiree(String emailPatient, String nomPatient,
                                       String nomMedecin, String date) {
        String sujet = "😔 Aucun créneau disponible - MediCare";
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#64748b,#94a3b8);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">🏥 MediCare</h1>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#64748b;">Bonjour %s,</h2>
                <p style="color:#555;font-size:16px;">
                  Malheureusement, <strong>aucun créneau ne s'est libéré</strong> avec le Dr. <strong>%s</strong> le %s.
                </p>
                <div style="background:#f1f5f9;border-left:4px solid #64748b;padding:20px;margin:20px 0;border-radius:8px;">
                  <p style="margin:8px 0;font-size:15px;">👨‍⚕️ <strong>Médecin :</strong> Dr. %s</p>
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Date demandée :</strong> %s</p>
                </div>
                <p style="color:#555;font-size:14px;">
                  Vous pouvez vous reconnecter sur <strong>MediCare</strong> pour chercher une autre date disponible.
                </p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomPatient, nomMedecin, date, nomMedecin, date);
        envoyerEmail(emailPatient, sujet, contenu);
    }

    // ── Notification au médecin : patient a reporté (modifié) ────────────
    public void envoyerReportParPatient(String emailMedecin, String nomMedecin,
                                         String nomPatient, String ancienneDate,
                                         String nouvelleDate, String nouvelleHeure) {
        String sujet = "🔄 Modification de rendez-vous par un patient - MediCare";
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#0d9488,#14b8a6);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">🏥 MediCare</h1>
                <p style="color:#ccfbf1;margin:5px 0 0;">Notification médecin</p>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#0d9488;">Bonjour Dr. %s,</h2>
                <p style="color:#555;font-size:16px;">
                  Le patient <strong>%s</strong> a <strong style="color:#f59e0b;">modifié</strong> son rendez-vous.
                </p>
                <div style="background:#FFF8E1;border-left:4px solid #f59e0b;padding:20px;margin:20px 0;border-radius:8px;">
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Ancienne date :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Nouvelle date :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">🕐 <strong>Nouvelle heure :</strong> %s</p>
                </div>
                <p style="color:#555;font-size:14px;">Connectez-vous sur MediCare pour voir les détails.</p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomMedecin, nomPatient, ancienneDate, nouvelleDate, nouvelleHeure);
        envoyerEmail(emailMedecin, sujet, contenu);
    }

    // ── Notification au médecin : patient a annulé ────────────────────────
    public void envoyerAnnulationParPatient(String emailMedecin, String nomMedecin,
                                             String nomPatient, String date, String heure) {
        String sujet = "🔔 Annulation de rendez-vous par un patient - MediCare";
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#0d9488,#14b8a6);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">🏥 MediCare</h1>
                <p style="color:#ccfbf1;margin:5px 0 0;">Notification médecin</p>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#0d9488;">Bonjour Dr. %s,</h2>
                <p style="color:#555;font-size:16px;">
                  Un patient a <strong style="color:#c62828;">annulé</strong> son rendez-vous.
                </p>
                <div style="background:#fff3cd;border-left:4px solid #f59e0b;padding:20px;margin:20px 0;border-radius:8px;">
                  <p style="margin:8px 0;font-size:15px;">👤 <strong>Patient :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">📅 <strong>Date annulée :</strong> %s</p>
                  <p style="margin:8px 0;font-size:15px;">🕐 <strong>Heure :</strong> %s</p>
                </div>
                <p style="color:#555;font-size:14px;">Ce créneau est désormais disponible pour un autre patient.</p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomMedecin, nomPatient, date, heure);
        envoyerEmail(emailMedecin, sujet, contenu);
    }

    // ── Remerciement au patient après évaluation ─────────────────────────
    public void envoyerRemerciementEvaluation(String emailPatient, String nomPatient,
                                               String nomMedecin, int note) {
        String sujet = "⭐ Merci pour votre évaluation - MediCare";
        String etoiles = "★".repeat(note) + "☆".repeat(5 - note);
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#f59e0b,#fbbf24);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">⭐ MediCare</h1>
                <p style="color:#fff7ed;margin:5px 0 0;">Merci pour votre retour</p>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#d97706;">Bonjour %s,</h2>
                <p style="color:#555;font-size:16px;">
                  Merci d'avoir pris le temps d'évaluer votre consultation avec le Dr. <strong>%s</strong>.
                  Votre avis aide d'autres patients à choisir le bon médecin et permet à votre praticien de progresser.
                </p>
                <div style="background:#FEF3C7;border-left:4px solid #f59e0b;padding:20px;margin:20px 0;border-radius:8px;text-align:center;">
                  <p style="margin:0;font-size:13px;color:#92400e;">Votre note globale</p>
                  <p style="margin:8px 0 0;font-size:34px;color:#f59e0b;letter-spacing:4px;">%s</p>
                  <p style="margin:6px 0 0;font-size:14px;color:#92400e;font-weight:bold;">%d / 5</p>
                </div>
                <p style="color:#555;font-size:14px;">À très bientôt sur <strong>MediCare</strong> !</p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomPatient, nomMedecin, etoiles, note);
        envoyerEmail(emailPatient, sujet, contenu);
    }

    // ── Notification au médecin : nouvelle évaluation reçue ──────────────
    public void envoyerNotificationEvaluation(String emailMedecin, String nomMedecin,
                                               String nomPatient, int note, String commentaire) {
        String sujet = "🌟 Nouvelle évaluation reçue - MediCare";
        String etoiles = "★".repeat(note) + "☆".repeat(5 - note);
        String blocCommentaire = (commentaire != null && !commentaire.isBlank())
                ? "<div style=\"background:#f8fafc;border-left:4px solid #64748b;padding:15px;margin:15px 0;border-radius:6px;font-style:italic;color:#334155;\">« " + commentaire.replace("<", "&lt;").replace(">", "&gt;") + " »</div>"
                : "<p style=\"color:#94a3b8;font-size:13px;font-style:italic;\">Aucun commentaire laissé.</p>";
        String contenu = """
            <div style="font-family:Arial,sans-serif;max-width:600px;margin:auto;border:1px solid #e0e0e0;border-radius:12px;overflow:hidden;">
              <div style="background:linear-gradient(135deg,#0d9488,#14b8a6);padding:25px;text-align:center;">
                <h1 style="color:white;margin:0;font-size:28px;">🌟 MediCare</h1>
                <p style="color:#ccfbf1;margin:5px 0 0;">Nouvelle évaluation</p>
              </div>
              <div style="padding:30px;">
                <h2 style="color:#0d9488;">Bonjour Dr. %s,</h2>
                <p style="color:#555;font-size:16px;">
                  Le patient <strong>%s</strong> vient de noter votre consultation.
                </p>
                <div style="background:#FEF3C7;border-left:4px solid #f59e0b;padding:20px;margin:20px 0;border-radius:8px;text-align:center;">
                  <p style="margin:0;font-size:34px;color:#f59e0b;letter-spacing:4px;">%s</p>
                  <p style="margin:6px 0 0;font-size:14px;color:#92400e;font-weight:bold;">%d / 5</p>
                </div>
                %s
                <p style="color:#555;font-size:14px;">Connectez-vous sur <strong>MediCare</strong> → onglet « Mes évaluations » pour consulter le détail des critères.</p>
              </div>
              <div style="background:#f5f5f5;padding:15px;text-align:center;">
                <p style="color:#999;font-size:12px;margin:0;">MediCare © 2026 — Ne pas répondre à cet email</p>
              </div>
            </div>
            """.formatted(nomMedecin, nomPatient, etoiles, note, blocCommentaire);
        envoyerEmail(emailMedecin, sujet, contenu);
    }
}

