package com.medicare.controllers;

import com.medicare.models.Don;
import com.medicare.services.DonationService;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DonationMapController {

    @FXML
    private WebView mapWebView;

    private final DonationService donationService = new DonationService();

    @FXML
    public void initialize() {
        WebEngine webEngine = mapWebView.getEngine();
        
        // Anti-clignotement : On utilise une couleur de fond solide au lieu de null 
        // pour éviter le flash blanc/noir lors du chargement.
        // #f8fafc est la couleur utilisée dans le CSS de la carte.
        // mapWebView.setPageFill(null); // Retiré pour éviter la transparence qui peut faire clignoter
        
        // Redirection console.log, console.error, etc.
        webEngine.setOnAlert(event -> {
            String msg = event.getData();
            if (msg.startsWith("LOG:")) System.out.println("[JS LOG] " + msg.substring(4));
            else if (msg.startsWith("ERROR:")) System.err.println("[JS ERROR] " + msg.substring(6));
            else if (msg.startsWith("WARN:")) System.out.println("[JS WARN] " + msg.substring(5));
            else System.out.println("[JS Alert] " + msg);
        });
        
        String debugScript = "window.console.log = function(msg) { alert('LOG:' + msg); }; " +
                           "window.console.error = function(msg) { alert('ERROR:' + msg); }; " +
                           "window.console.warn = function(msg) { alert('WARN:' + msg); };";
        
        // Configuration de la carte Leaflet
        String htmlContent = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>Carte des Dons</title>
                <meta charset="utf-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
                <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
                <style>
                    html, body { height: 100%; margin: 0; padding: 0; background: #f8fafc; overflow: hidden; }
                    #map { position: absolute; top: 0; bottom: 0; left: 0; right: 0; background: #f8fafc; }
                    .custom-popup { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; min-width: 150px; }
                    .popup-title { font-weight: bold; color: #10b981; margin-bottom: 5px; border-bottom: 1px solid #eee; padding-bottom: 5px; }
                    .popup-stat { font-size: 13px; margin: 3px 0; }
                    .popup-stat b { color: #1e293b; }
                    .leaflet-container { background: #f8fafc !important; outline: 0; }
                </style>
            </head>
            <body>
                <div id="map"></div>
                <script>
                    var map;
                    var markers = [];

                    function initMap() {
                        console.log("JS: initMap starting...");
                        try {
                            map = L.map('map', {
                                fadeAnimation: true,
                                zoomAnimation: true,
                                markerZoomAnimation: true,
                                preferCanvas: true,
                                zoomControl: true
                            }).setView([33.8869, 9.5375], 6);
                            
                            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                attribution: '&copy; OpenStreetMap contributors',
                                maxZoom: 19
                            }).addTo(map);

                            // Correction immédiate pour le rendu
                            setTimeout(function() {
                                map.invalidateSize();
                                console.log("JS: map size invalidated");
                            }, 100);
                        } catch(e) {
                            console.error("Map Init Error: " + e.message);
                        }
                    }

    function addMarkers(donationsJson) {
                        console.log("JS: addMarkers received: " + donationsJson);
                        try {
                            // Nettoyer proprement les anciens marqueurs
                            if (markers && markers.length > 0) {
                                markers.forEach(m => map.removeLayer(m));
                            }
                            markers = [];

                            var locations = JSON.parse(donationsJson);
                            if (!locations || locations.length === 0) {
                                console.warn("JS: No locations to display");
                                return;
                            }
                            
                            console.log("JS: adding " + locations.length + " markers...");
                            locations.forEach(function(loc) {
                                if (loc.lat && loc.lng) {
                                    var marker = L.marker([loc.lat, loc.lng]).addTo(map);
                                    var popupContent = '<div class="custom-popup">' +
                                                     '<div class="popup-title">' + loc.name + '</div>' +
                                                     '<div class="popup-stat"><b>Dons:</b> ' + loc.count + '</div>' +
                                                     '<div class="popup-stat"><b>Total:</b> ' + loc.totalInfo + '</div>' +
                                                     '</div>';
                                    marker.bindPopup(popupContent);
                                    markers.push(marker);
                                }
                            });
                        } catch(e) {
                            console.error("JS: Add Markers Error: " + e.message);
                        }
                    }

                    initMap();
                </script>
            </body>
            </html>
            """;

        webEngine.loadContent(htmlContent);

        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                // Injecter le script de debug
                webEngine.executeScript(debugScript);
                
                // Petit délai pour s'assurer que Leaflet est prêt
                javafx.application.Platform.runLater(() -> {
                    loadDonationData(webEngine);
                });
            }
        });
    }

    private void loadDonationData(WebEngine webEngine) {
        System.out.println("DonationMapController: Début loadDonationData");
        List<Don> allDons = donationService.getAllDons();
        
        if (allDons == null || allDons.isEmpty()) {
            System.err.println("DonationMapController: ATTENTION - Aucun don trouvé dans la base de données !");
            // On envoie un tableau vide pour nettoyer la carte
            webEngine.executeScript("addMarkers('[]')");
            return;
        }
        
        System.out.println("DonationMapController: " + allDons.size() + " dons récupérés");
        
        // Regrouper les dons par position (lat, lng)
        Map<String, List<Don>> groupedDons = new HashMap<>();
        
        // Coordonnées approximatives pour les pays (Fallback)
        Map<String, double[]> countryCoords = new HashMap<>();
        countryCoords.put("Tunisie", new double[]{33.8869, 9.5375});
        countryCoords.put("France", new double[]{46.2276, 2.2137});
        countryCoords.put("Maroc", new double[]{31.7917, -7.0926});
        countryCoords.put("Algérie", new double[]{28.0339, 1.6596});
        countryCoords.put("Canada", new double[]{56.1304, -106.3468});
        countryCoords.put("USA", new double[]{37.0902, -95.7129});
        countryCoords.put("Allemagne", new double[]{51.1657, 10.4515});
        countryCoords.put("Espagne", new double[]{40.4637, -3.7492});
        countryCoords.put("Italie", new double[]{41.8719, 12.5674});
        countryCoords.put("Belgique", new double[]{50.5039, 4.4699});
        countryCoords.put("Suisse", new double[]{46.8182, 8.2275});
        countryCoords.put("Royaume-Uni", new double[]{55.3781, -3.4360});
        countryCoords.put("Libye", new double[]{26.3351, 17.2283});
        countryCoords.put("Égypte", new double[]{26.8206, 30.8025});

        for (Don don : allDons) {
            double lat = 0;
            double lng = 0;
            String locationName = "";

            if (don.getLatitude() != null && don.getLongitude() != null && 
                (don.getLatitude() != 0 || don.getLongitude() != 0)) {
                lat = don.getLatitude();
                lng = don.getLongitude();
                locationName = don.getAdresse() != null ? don.getAdresse() : "Position précise";
                System.out.println("DonationMapController: Don ID " + don.getId() + " a des coordonnées réelles: " + lat + ", " + lng);
            } else {
                String country = extractCountry(don.getAdresse());
                double[] coords = countryCoords.get(country);
                if (coords != null) {
                    lat = coords[0];
                    lng = coords[1];
                    locationName = country;
                    System.out.println("DonationMapController: Don ID " + don.getId() + " utilisé fallback pays: " + country);
                } else {
                    System.out.println("DonationMapController: Don ID " + don.getId() + " n'a ni coordonnées ni pays reconnu (Adresse: " + don.getAdresse() + ")");
                }
            }

            if (lat != 0 || lng != 0) {
                // Utilisation de Locale.US pour garantir le point comme séparateur décimal
                String key = String.format(java.util.Locale.US, "%.8f|%.8f|%s", lat, lng, locationName);
                groupedDons.computeIfAbsent(key, k -> new java.util.ArrayList<>()).add(don);
            }
        }

        StringBuilder jsonBuilder = new StringBuilder("[");
        boolean firstLoc = true;

        for (Map.Entry<String, List<Don>> entry : groupedDons.entrySet()) {
            try {
                String[] keyParts = entry.getKey().split("\\|");
                double lat = Double.parseDouble(keyParts[0]);
                double lng = Double.parseDouble(keyParts[1]);
                String name = keyParts[2];
                List<Don> dons = entry.getValue();

                if (!firstLoc) jsonBuilder.append(",");
                
                double totalMoney = dons.stream()
                        .filter(d -> "argent".equals(d.getType()))
                        .mapToDouble(d -> d.getMontant() != null ? d.getMontant() : 0.0)
                        .sum();
                
                long materialCount = dons.stream()
                        .filter(d -> "materiel".equals(d.getType()))
                        .count();
                
                String totalInfo = "";
                if (totalMoney > 0 && materialCount > 0) {
                    totalInfo = String.format("%.2f DT + %d matériels", totalMoney, materialCount);
                } else if (totalMoney > 0) {
                    totalInfo = String.format("%.2f DT", totalMoney);
                } else {
                    totalInfo = String.format("%d matériels", materialCount);
                }

                jsonBuilder.append(String.format(
                    java.util.Locale.US,
                    "{\"lat\":%.8f, \"lng\":%.8f, \"name\":\"%s\", \"count\":%d, \"totalInfo\":\"%s\"}",
                    lat, lng, escapeJson(name), dons.size(), escapeJson(totalInfo)
                ));
                firstLoc = false;
            } catch (Exception e) {
                System.err.println("DonationMapController: Erreur lors du groupement JSON: " + e.getMessage());
            }
        }
        jsonBuilder.append("]");
        
        String finalJson = jsonBuilder.toString();
        System.out.println("DonationMapController: JSON FINAL ENVOYÉ: " + finalJson);
        
        javafx.application.Platform.runLater(() -> {
            try {
                webEngine.executeScript("addMarkers('" + finalJson.replace("'", "\\'") + "')");
            } catch (Exception e) {
                System.err.println("DonationMapController: Erreur CRITIQUE executeScript: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\b", "\\b")
                    .replace("\f", "\\f")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
        // Ne PAS échapper les quotes simples ici, 
        // elles seront gérées globalement lors de l'appel executeScript
    }

    private String extractCountry(String address) {
        if (address == null || address.isEmpty()) return "Inconnu";
        
        String addrLower = address.toLowerCase();
        if (addrLower.contains("tunisie") || addrLower.contains("tunisia") || addrLower.contains("tunis")) return "Tunisie";
        if (addrLower.contains("france")) return "France";
        if (addrLower.contains("maroc") || addrLower.contains("morocco")) return "Maroc";
        if (addrLower.contains("algérie") || addrLower.contains("algeria")) return "Algérie";
        if (addrLower.contains("canada")) return "Canada";
        if (addrLower.contains("usa") || addrLower.contains("états-unis") || addrLower.contains("united states")) return "USA";
        if (addrLower.contains("allemagne") || addrLower.contains("germany")) return "Allemagne";
        if (addrLower.contains("espagne") || addrLower.contains("spain")) return "Espagne";
        if (addrLower.contains("italie") || addrLower.contains("italy")) return "Italie";
        if (addrLower.contains("belgique") || addrLower.contains("belgium")) return "Belgique";
        if (addrLower.contains("suisse") || addrLower.contains("switzerland")) return "Suisse";
        if (addrLower.contains("uk") || addrLower.contains("royaume-uni") || addrLower.contains("united kingdom")) return "Royaume-Uni";
        if (addrLower.contains("libye") || addrLower.contains("libya")) return "Libye";
        if (addrLower.contains("égypte") || addrLower.contains("egypt")) return "Égypte";
        
        // Par défaut, on retourne la dernière partie de l'adresse si séparée par des virgules
        String[] parts = address.split(",");
        return parts[parts.length - 1].trim();
    }

    @FXML
    private void onBackClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/medicare/admin-donation-view.fxml"));
            StackPane contentArea = (StackPane) mapWebView.getScene().lookup("#contentArea");
            contentArea.getChildren().clear();
            contentArea.getChildren().add(loader.load());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
