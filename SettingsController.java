package com.Controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.net.URL;
import java.util.ResourceBundle;

public class SettingsController implements Initializable {

    @FXML private TextFlow userManualFlow;
    @FXML private ScrollPane scrollPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {

        UserManualFactory.addUserManualContent(userManualFlow);

        // Set TextFlow background to white
        userManualFlow.setStyle("-fx-background-color: white; -fx-padding: 10;");

        // Set ScrollPane background to white
        scrollPane.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-insets: 0;" +
                        "-fx-background-radius: 0;"
        );

        scrollPane.setFitToWidth(true);
    }


    public static class UserManualFactory {

        public static void addUserManualContent(TextFlow flow) {
            flow.setLineSpacing(5);
            flow.setPrefWidth(600);
            flow.setStyle("-fx-font-family: 'Poppins'; -fx-font-size: 14px;");

            // Sections
            flow.getChildren().addAll(
                    createHeader("How to Add a Car"),
                    createSteps(
                            "1. Navigate to the Vehicles tab.\n" +
                                    "2. Click on the 'Add Car' button.\n" +
                                    "3. Fill out the required fields (Plate Number, Owner, Contact, Brand/Model, etc.).\n" +
                                    "4. Select a Vehicle Type and a Slot and set Duration (in Hours).\n" +
                                    "5. Click 'Save' to confirm.\n" +
                                    "---------------------------------------------------------------------------------\n\n"
                    ),

                    createHeader("How to Edit a Car"),
                    createSteps(
                            "1. Go to the Vehicles tab.\n" +
                                    "2. Select the car you want to edit from the list.\n" +
                                    "3. Click the 'Edit' button.\n" +
                                    "4. Modify the details as needed, then click 'Save'.\n" +
                                    "---------------------------------------------------------------------------------\n\n"
                    ),

                    createHeader("How to Remove a Car Temporarily"),
                    createSteps(
                            "1. In the Vehicles tab, select the car.\n" +
                                    "2. Click 'Remove'.\n" +
                                    "3. Go to the Trash Tab to see the removed vehicle.\n" +
                                    "4. The car will move to the Trash but is not permanently deleted.\n" +
                                    "NOTE: Only Remove a Vehicle if 'OVERDUE'.\n" +
                                    "---------------------------------------------------------------------------------\n\n"
                    ),

                    createHeader("How to Restore a Removed Car"),
                    createSteps(
                            "1. Navigate to the Trash tab.\n" +
                                    "2. Select the car you want to restore.\n" +
                                    "3. Click 'Restore' to return it to Vehicles.\n" +
                                    "4. You should see the restored vehicle with an 'EXITED' status.\n" +
                                    "---------------------------------------------------------------------------------\n\n"
                    ),

                    createHeader("How to Permanently Delete a Car"),
                    createSteps(
                            "1. Open the Trash tab.\n" +
                                    "2. Select the car you want to delete.\n" +
                                    "3. Click 'Delete Permanently'.\n" +
                                    "WARNING: This action cannot be undone.\n" +
                                    "---------------------------------------------------------------------------------\n\n"
                    ),

                    createHeader("How to Print a Receipt"),
                    createSteps(
                            "1. Open the Transactions tab.\n" +
                                    "2. Select the completed transaction.\n" +
                                    "3. Click 'Print Receipt'.\n" +
                                    "4. Wait for the receipt to be complete..\n" +
                                    "---------------------------------------------------------------------------------\n\n"
                    ),

                    createHeader("How to Navigate the Parking Lot Map"),
                    createSteps(
                            "1. Go to the Parking Lots tab.\n" +
                                    "2. Hover over or click a slot to see its details.\n" +
                                    "3. Use the scroll and select parking area to navigate the map.\n" +
                                    "---------------------------------------------------------------------------------\n\n"
                    ),

                    createHeader("How to Generate Weekly Reports"),
                    createSteps(
                            "1. Go to the Reports tab.\n" +
                                    "2. Select Date Range (Weekly)'.\n" +
                                    "3. Choose output format (CSV or PDF).\n" +
                                    "4. Click 'Generate'.\n" +
                                    "---------------------------------------------------------------------------------\n\n"
                    ),

                    createHeader("How to Refund/Adjust"),
                    createSteps(
                            "1. Go to the Transactions tab.\n" +
                                    "2. Search and Press for the Customer.\n" +
                                    "3. Press 'Adjust/Refund' and set new Duration.\n" +
                                    "4. Print Receipt After .\n" +
                                    "---------------------------------------------------------------------------------\n\n"
                    )
            );
        }

        private static Text createHeader(String text) {
            Text t = new Text(text + "\n");
            t.setStyle("-fx-font-weight: bold; -fx-font-size: 20px;");
            return t;
        }

        private static Text createSteps(String text) {
            Text t = new Text(text);
            t.setStyle("-fx-font-size: 14px; -fx-font-family: 'Poppins';");
            return t;
        }
    }

}
