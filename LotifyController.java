package com.Controllers;

import com.lotify.lotify.Notification;
import com.lotify.lotify.NotificationManager;
import com.lotify.lotify.NotificationScheduler;
import javafx.animation.*;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.*;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.ResourceBundle;

public class LotifyController implements javafx.fxml.Initializable {

    @FXML private AnchorPane sidebarPane;
    @FXML private StackPane contentArea;
    @FXML private BorderPane mainPane;
    @FXML private Button notificationButton;
    @FXML private Circle notificationDot;
    @FXML private Label dateTimeLabel;

    private Popup notificationPopup;
    private VBox notificationContent;
    private boolean sidebarVisible = false;
    private final double SIDEBAR_WIDTH = 300.0;
    private final Duration ANIM_DURATION = Duration.millis(300);

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        startDateTimeUpdater();

        sidebarPane.setMinWidth(0);
        sidebarPane.setMaxWidth(SIDEBAR_WIDTH);
        sidebarPane.setPrefWidth(0);
        sidebarPane.setManaged(false);
        sidebarPane.setVisible(false);
        sidebarVisible = false;

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(sidebarPane.widthProperty());
        clip.heightProperty().bind(sidebarPane.heightProperty());
        sidebarPane.setClip(clip);

        loadPage("Dashboard.fxml");
        setupNotificationPopup();

        NotificationManager.getNotifications().addListener((ListChangeListener<Notification>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    playNotificationSound();
                }
            }
            refreshNotificationPopup();
            setNotificationBadgeVisible(true);
        });
        notificationButton.setOnAction(event -> toggleNotificationPopup());
        refreshNotificationPopup();
        NotificationScheduler.reloadFromDatabase();
    }

    private void toggleNotificationPopup() {
        if (notificationPopup.isShowing()) {
            TranslateTransition slideOut = new TranslateTransition(Duration.millis(200), notificationContent);
            slideOut.setFromY(0);
            slideOut.setToY(-10);

            FadeTransition fadeOut = new FadeTransition(Duration.millis(200), notificationContent);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);

            slideOut.setOnFinished(e -> notificationPopup.hide());
            slideOut.play();
            fadeOut.play();
        } else {
            Bounds bounds = notificationButton.localToScreen(notificationButton.getBoundsInLocal());
            notificationPopup.show(
                    notificationButton.getScene().getWindow(),
                    bounds.getMinX(),
                    bounds.getMaxY()
            );
            notificationDot.setVisible(false);
            notificationContent.setTranslateY(-10);
            notificationContent.setOpacity(0);

            TranslateTransition slideIn = new TranslateTransition(Duration.millis(200), notificationContent);
            slideIn.setFromY(-10);
            slideIn.setToY(0);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), notificationContent);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);

            slideIn.play();
            fadeIn.play();
        }
    }

    private void setupNotificationPopup() {
        VBox contentBox = new VBox(10);
        contentBox.setPadding(new Insets(10));
        contentBox.setStyle(
                "-fx-background-color: white; -fx-border-color: #ccc; " +
                        "-fx-border-radius: 8; -fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);"
        );
        contentBox.setPrefWidth(400);

        Button clearAll = new Button("Clear All");
        clearAll.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
        clearAll.setMaxWidth(Double.MAX_VALUE);
        clearAll.setOnAction(e -> {
            NotificationManager.clear();
            refreshNotificationPopup();
            setNotificationBadgeVisible(false);
        });

        contentBox.getChildren().add(clearAll);

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        notificationContent = contentBox;

        notificationPopup = new Popup();
        notificationPopup.getContent().add(scrollPane);
        notificationPopup.setAutoHide(true);
    }

    private void refreshNotificationPopup() {
        // Remove old notification nodes (but keep "Clear All" button at index 0)
        notificationContent.getChildren().removeIf(node -> node instanceof HBox || node instanceof Label);

        if (NotificationManager.getNotifications().isEmpty()) {
            Label none = new Label("No notifications");
            none.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            notificationContent.getChildren().add(none);
        } else {
            for (Notification n : NotificationManager.getNotifications()) {
                Label msgLabel = new Label(n.getFormattedMessage());

                String color = switch (n.getType()) {
                    case OVERDUE -> "#e53935";
                    case INFO -> "#1e88e5";
                    case UPDATE -> "#8e24aa";
                    case EXITED -> "#43a047";
                    case ERROR -> "#f4511e";
                    default -> "#333333";
                };

                msgLabel.setWrapText(true);
                msgLabel.setMaxWidth(380);
                msgLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px; -fx-font-weight: bold;");

                Label timeLabel = new Label(n.getFormattedTimestamp());
                timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-size: 10px;");

                VBox content = new VBox(msgLabel, timeLabel);
                content.setSpacing(2);

                HBox wrapper = new HBox(content);
                wrapper.setStyle("-fx-padding: 8 0 8 0; -fx-border-color: #eee; -fx-border-width: 0 0 1 0;");

                // ✅ Append directly, don't force index
                notificationContent.getChildren().add(wrapper);
            }
        }
    }

    private void setNotificationBadgeVisible(boolean visible) {
        if (notificationDot != null) {
            notificationDot.setVisible(visible);
        }
    }
    private void startDateTimeUpdater() {
        Timeline clock = new Timeline(new KeyFrame(Duration.ZERO, e -> {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE: [MM-dd-yyyy] - HH:mm");
            dateTimeLabel.setText(now.format(formatter));

            String textColor = "#000000";

            dateTimeLabel.setStyle(
                    "-fx-text-fill: " + textColor + ";" +
                            "-fx-font-family: 'Poppins';" +
                            "-fx-font-size: 32px;" +
                            "-fx-font-weight: BOLD;" +
                            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"
            );

        }), new KeyFrame(Duration.seconds(30)));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void playNotificationSound() {
        String soundPath = getClass().getResource("/sounds/notification.mp3").toString();
        AudioClip clip = new AudioClip(soundPath);
        clip.play();
    }


    @FXML private void toggleSidebar() {
        if (sidebarVisible) {
            // Fade out children
            FadeTransition fadeOut = new FadeTransition(ANIM_DURATION, sidebarPane);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            // Collapse width
            Timeline collapse = new Timeline(
                    new KeyFrame(ANIM_DURATION,
                            new KeyValue(sidebarPane.prefWidthProperty(), 0, Interpolator.EASE_BOTH)
                    )
            );

            ParallelTransition closeAnim = new ParallelTransition(fadeOut, collapse);
            closeAnim.setOnFinished(e -> {
                sidebarPane.setManaged(false);
                sidebarPane.setVisible(false);
                sidebarPane.setOpacity(1.0); // reset so it's ready for next expand
            });
            closeAnim.play();

        } else {
            // Prepare sidebar before animation
            sidebarPane.setManaged(true);
            sidebarPane.setVisible(true);
            sidebarPane.setPrefWidth(0);
            sidebarPane.setOpacity(0.0);

            // Expand width
            Timeline expand = new Timeline(
                    new KeyFrame(ANIM_DURATION,
                            new KeyValue(sidebarPane.prefWidthProperty(), SIDEBAR_WIDTH, Interpolator.EASE_BOTH)
                    )
            );

            // Fade in children
            FadeTransition fadeIn = new FadeTransition(ANIM_DURATION, sidebarPane);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);

            ParallelTransition openAnim = new ParallelTransition(expand, fadeIn);
            openAnim.play();
        }

        sidebarVisible = !sidebarVisible;
    }
    @FXML private void exitApp() {
        System.exit(0);
    }

    @FXML private void showDashboard()     { loadPage("Dashboard.fxml"); }
    @FXML private void showVehicles()      { loadPage("Vehicles.fxml"); }
    @FXML private void showParkingSlots()  { loadPage("ParkingSlots.fxml"); }
    @FXML private void showTransactions()  { loadPage("Transactions.fxml"); }
    @FXML private void showReports()       { loadPage("Reports.fxml"); }
    @FXML private void showSettings()      { loadPage("Settings.fxml"); }
    @FXML private void handleOpenTrash() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lotify/lotify/trash-dialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initStyle(StageStyle.UNDECORATED);
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(mainPane.getScene().getWindow()); // FIXED
            dialogStage.setScene(new Scene(root));
            dialogStage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadPage(String fxmlFile) {
        try {
            Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/com/lotify/lotify/" + fxmlFile)));
            contentArea.getChildren().clear();
            contentArea.getChildren().add(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
