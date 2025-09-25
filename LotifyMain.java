package com.lotify.lotify;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCombination;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class LotifyMain extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/lotify/lotify/MainFrame.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/com/lotify/lotify/mainframeStyles.css").toExternalForm());
        scene.getStylesheets().add(getClass().getResource("/com/lotify/lotify/chartstyles.css").toExternalForm());

        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setTitle("Lotify");

        stage.getIcons().add(new Image(getClass().getResourceAsStream("/images/LotifyLogo.png")));

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
