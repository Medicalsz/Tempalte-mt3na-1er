package com.medicare.controllers;

import com.medicare.models.Collaboration;
import com.medicare.models.Score;
import com.medicare.models.User;
import com.medicare.services.CollaborationService;
import com.medicare.services.LeaderboardService;
import com.medicare.services.UserService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatisticsController {

    @FXML
    private PieChart statusPieChart;
    @FXML
    private PieChart userRolesPieChart;
    @FXML
    private Label gamesPlayedLabel;
    @FXML
    private Label bestTimeLabel;

    private final CollaborationService collaborationService = new CollaborationService();
    private final LeaderboardService leaderboardService = new LeaderboardService();
    private final UserService userService = new UserService();
    private DashboardAdminController dashboardController;

    public void setDashboardController(DashboardAdminController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML
    private void initialize() {
        loadCollaborationStats();
        loadGameStats();
        loadUserRoleStats();
    }

    private void loadCollaborationStats() {
        List<Collaboration> collaborations = collaborationService.getAll();
        Map<String, Long> statusCounts = collaborations.stream()
                .collect(Collectors.groupingBy(Collaboration::getStatut, Collectors.counting()));

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Long> entry : statusCounts.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }

        statusPieChart.setData(pieChartData);
        statusPieChart.setClockwise(true);
        statusPieChart.setLabelLineLength(50);
        statusPieChart.setLabelsVisible(true);
        statusPieChart.setStartAngle(90);

        // Add click handlers to each pie slice for interactive filtering
        for (PieChart.Data data : statusPieChart.getData()) {
            data.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if (dashboardController != null) {
                    // Extract status from "Status (Count)" format
                    String status = data.getName().split(" \\(")[0];
                    dashboardController.navigateToCollaborationsWithFilter(status);
                }
            });
        }
    }

    private void loadGameStats() {
        int totalGames = leaderboardService.getTotalGamesPlayed();
        gamesPlayedLabel.setText(String.valueOf(totalGames));

        List<Score> topScores = leaderboardService.getTopScores();
        if (topScores.isEmpty()) {
            bestTimeLabel.setText("N/A");
        } else {
            // Assuming the first score is the best
            bestTimeLabel.setText(topScores.get(0).getFormattedTime());
        }
    }

    private void loadUserRoleStats() {
        List<User> users = userService.getAllUsers();
        // The roles are stored as a JSON string like ["ROLE_ADMIN"], so we parse it simply.
        Map<String, Long> roleCounts = users.stream()
                .map(user -> {
                    String roles = user.getRoles();
                    if (roles.contains("ROLE_ADMIN")) return "Admin";
                    if (roles.contains("ROLE_MEDECIN")) return "Médecin";
                    if (roles.contains("ROLE_PATIENT")) return "Patient";
                    return "Utilisateur"; // Default/Fallback
                })
                .collect(Collectors.groupingBy(role -> role, Collectors.counting()));

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        for (Map.Entry<String, Long> entry : roleCounts.entrySet()) {
            pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }

        userRolesPieChart.setData(pieChartData);
        userRolesPieChart.setClockwise(true);
        userRolesPieChart.setLabelLineLength(50);
        userRolesPieChart.setLabelsVisible(true);
        userRolesPieChart.setStartAngle(90);
    }
}
