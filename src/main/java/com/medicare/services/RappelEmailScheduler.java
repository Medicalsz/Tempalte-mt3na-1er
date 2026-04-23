package com.medicare.services;

import com.medicare.models.ListeAttente;
import com.medicare.models.RendezVous;
import com.medicare.models.User;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RappelEmailScheduler {

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();
    private final EmailService         emailService        = new EmailService();
    private final RendezVousService    rdvService          = new RendezVousService();
    private final UserService          userService         = new UserService();
    private final ListeAttenteService  listeAttenteService = new ListeAttenteService();

    public void demarrer() {
        scheduler.scheduleAtFixedRate(this::verifierEtEnvoyerRappels, 0, 1, TimeUnit.HOURS);
        scheduler.scheduleAtFixedRate(this::verifierAttenteExpiree, 0, 1, TimeUnit.HOURS);
        System.out.println("✅ Scheduler rappels email démarré");
    }

    public void arreter() {
        scheduler.shutdownNow();
    }

    private void verifierEtEnvoyerRappels() {
        try {
            LocalDate demain = LocalDate.now().plusDays(1);
            // Récupère uniquement les RDV confirmés de demain dont le rappel n'a pas encore été envoyé
            List<RendezVous> rdvDemain = rdvService.getRendezVousConfirmesParDate(demain);

            for (RendezVous rv : rdvDemain) {
                User patient = userService.getUserByPatientId(rv.getPatientId());
                if (patient != null && patient.getEmail() != null) {
                    String nomMedecin = rv.getMedecinPrenom() + " " + rv.getMedecinNom();
                    emailService.envoyerRappelRdv(
                            patient.getEmail(),
                            patient.getPrenom() + " " + patient.getNom(),
                            nomMedecin,
                            rv.getDate().toString(),
                            rv.getHeure().toString()
                    );
                    // Marquer comme envoyé pour éviter les doublons
                    rdvService.markRappelEnvoye(rv.getId());
                    System.out.println("✅ Rappel envoyé pour RDV #" + rv.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur scheduler rappels : " + e.getMessage());
        }
    }

    // Vérifie les inscriptions expirées et notifie les patients
    private void verifierAttenteExpiree() {
        try {
            List<ListeAttente> expirees = listeAttenteService.getExpireesNonNotifiees();
            for (ListeAttente la : expirees) {
                User patient = userService.getUserByPatientId(la.getPatientId());
                if (patient != null && patient.getEmail() != null) {
                    String nomMedecin = la.getMedecinPrenom() + " " + la.getMedecinNom();
                    emailService.envoyerAttenteExpiree(
                            patient.getEmail(),
                            patient.getPrenom() + " " + patient.getNom(),
                            nomMedecin,
                            la.getDate().toString()
                    );
                    listeAttenteService.marquerNotifie(la.getId());
                    System.out.println("✅ Email expiration attente envoyé à : " + patient.getEmail());
                }
            }
            listeAttenteService.supprimerExpireesNotifiees();
        } catch (Exception e) {
            System.err.println("Erreur scheduler attente expirée : " + e.getMessage());
        }
    }
}
