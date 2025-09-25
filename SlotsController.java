package com.Controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

public class SlotsController {

    @FXML private Label slotInfoLabel;

    @FXML
    private void handleSlotClick(MouseEvent event) {
        StackPane clickedSlot = (StackPane) event.getSource();

        String slotId = clickedSlot.getId();

        // Find the rectangle inside the StackPane
        Rectangle rect = null;
        for (var node : clickedSlot.getChildren()) {
            if (node instanceof Rectangle) {
                rect = (Rectangle) node;
                break;
            }
        }

        if (rect != null) {
            // Toggle color (for demo)
            if ("#00ff00".equals(rect.getStroke().toString())) {
                rect.setStroke(javafx.scene.paint.Color.RED);
            } else {
                rect.setStroke(javafx.scene.paint.Color.GREEN);
            }
        }

        // Optional: show label somewhere
        if (slotInfoLabel != null) {
            slotInfoLabel.setText("Selected Slot: " + slotId);
        }

        System.out.println("Clicked slot: " + slotId);
    }
}
