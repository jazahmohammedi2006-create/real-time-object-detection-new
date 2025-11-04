package com.objectdetection;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class NoOpenCVTest extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("JavaFX is working! OpenCV needs setup.");
        Scene scene = new Scene(new StackPane(label), 300, 200);
        stage.setScene(scene);
        stage.setTitle("Test - No OpenCV");
        stage.show();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
