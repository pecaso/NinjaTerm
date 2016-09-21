package ninja.mbedded.ninjaterm.view.led;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.IOException;

/**
 * Controller for the "StatsView" sub-tab which is part of a terminal tab.
 *
 * @author Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 * @since 2016-09-16
 * @last-modified 2016-09-16
 */
public class Led extends StackPane {

    //================================================================================================//
    //========================================== FXML BINDINGS =======================================//
    //================================================================================================//

    @FXML
    public Circle persistentCircle;

    @FXML
    public Circle flashingCircle;

    //================================================================================================//
    //=========================================== CLASS FIELDS =======================================//
    //================================================================================================//

    private double radius;

    private FadeTransition fade;

    //================================================================================================//
    //========================================== CLASS METHODS =======================================//
    //================================================================================================//

    public void setColor(Color color) {

        persistentCircle.setStroke(color);
        persistentCircle.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, color, 10, 0, 0, 0));

        flashingCircle.setFill(color);
        flashingCircle.setEffect(new DropShadow(BlurType.THREE_PASS_BOX, color, 15, 0, 0, 0));

    }

    public Color getColor() {
        return (Color) flashingCircle.getFill();
    }

    public void setRadius(double radius) {
        this.radius = radius;

        persistentCircle.setRadius(radius);
        flashingCircle.setRadius(radius);

    }

    public double getRadius() {
        return radius;
    }


    public Led() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "LedView.fxml"));

        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }

        //==============================================//
        //========== PERSISTANT CIRCLE INIT ============//
        //==============================================//
        persistentCircle.setFill(Color.TRANSPARENT);
        persistentCircle.setStroke(Color.RED);
        persistentCircle.setStrokeWidth(2);


        //==============================================//
        //============= FADING CIRCLE INIT =============//
        //==============================================//

        flashingCircle.setOpacity(0.0);

        fade = new FadeTransition(Duration.seconds(0.5), flashingCircle);
        fade.setInterpolator(Interpolator.EASE_OUT);
        fade.setFromValue(1.0);
        fade.setToValue(0);
        //fade.setAutoReverse(true);
        fade.setCycleCount(1);





    }

    public void init() {

    }

    public void flash() {
        fade.stop();
        fade.play();
    }

}
