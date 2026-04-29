package com.medicare.services;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    // Identifiants configurés
    private final String username = "samermfarrej@gmail.com";
    private final String password = "sxrf prdu ajfn mnqj";

    public void sendVerificationCode(String toEmail, String code) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Code de vérification pour votre don - Medicare");
            message.setText("Bonjour,\n\n"
                    + "Vous avez initié un don d'un montant supérieur à 500 DT sur Medicare.\n"
                    + "Pour confirmer votre don, veuillez utiliser le code de vérification suivant : " + code + "\n\n"
                    + "Si vous n'êtes pas à l'origine de cette demande, veuillez ignorer cet e-mail.\n\n"
                    + "L'équipe Medicare");

            Transport.send(message);
            System.out.println("Email envoyé avec succès à " + toEmail);

        } catch (MessagingException e) {
            System.err.println("Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
