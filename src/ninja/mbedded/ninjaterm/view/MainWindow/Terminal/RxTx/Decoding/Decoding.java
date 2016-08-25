package ninja.mbedded.ninjaterm.view.MainWindow.Terminal.RxTx.Decoding;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import ninja.mbedded.ninjaterm.util.Decoding.DecodingOptions;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller for the Decoding pop-up window.
 *
 * @author          Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 * @since           2016-08-24
 * @last-modified   2016-08-25
 */
public class Decoding extends VBox {

    //================================================================================================//
    //========================================== FXML BINDINGS =======================================//
    //================================================================================================//

    @FXML
    public Label decodingLabel;

    @FXML
    public ComboBox<DecodingOptions> decodingComboBox;


    //================================================================================================//
    //=========================================== CLASS FIELDS =======================================//
    //================================================================================================//



    //================================================================================================//
    //========================================== CLASS METHODS =======================================//
    //================================================================================================//

    public Decoding() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "Decoding.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        // Populate
        decodingComboBox.getItems().setAll(DecodingOptions.values());

    }

}