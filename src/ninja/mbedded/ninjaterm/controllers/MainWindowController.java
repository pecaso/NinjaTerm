package ninja.mbedded.ninjaterm.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the main window of NinjaTerm.
 *
 * @author Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 * @last-modified 2016-07-10
 * @since 2016-07-08
 */
public class MainWindowController implements Initializable {

    //================================================================================================//
    //========================================== FXML BINDINGS =======================================//
    //================================================================================================//

    @FXML
    public Button openCloseComPortButton;

    @FXML
    public ComSettingsController comSettingsController;

    @FXML
    public StatusBarController statusBarController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {


        comSettingsController.setStatusBarController(statusBarController);

        openCloseComPortButton.setOnAction((ActionEvent) -> {
            openCloseComPortButtonPressed();
        });

        // Attach handler for when selected COM port changes. This is responsible for
        // enabling/disabling the "Open" button as appropriate
        comSettingsController.foundComPortsComboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("ComboBox selected item changed.");

            // newValue will be null if a scan was done and no COM ports
            // were found
            if (newValue == null) {
                openCloseComPortButton.setDisable(true);
            } else {
                openCloseComPortButton.setDisable(false);
            }
        });
    }

    private void openCloseComPortButtonPressed() {

        System.out.println("Button pressed handler called.");

        if (openCloseComPortButton.getText().equals("Open")) {
            openCloseComPortButton.setText("Close");
        } else {
            openCloseComPortButton.setText("Open");
        }

    }


}
