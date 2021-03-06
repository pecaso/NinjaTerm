package ninja.mbedded.ninjaterm.util.javafx.comDataPaneWeb;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import ninja.mbedded.ninjaterm.util.loggerUtils.LoggerUtils;
import ninja.mbedded.ninjaterm.util.rxProcessing.Marker;
import ninja.mbedded.ninjaterm.util.rxProcessing.ansiECParser.ColourMarker;
import ninja.mbedded.ninjaterm.util.rxProcessing.newLineParser.NewLineMarker;
import ninja.mbedded.ninjaterm.util.rxProcessing.streamedData.StreamedData;
import ninja.mbedded.ninjaterm.util.rxProcessing.timeStamp.TimeStampMarker;
import ninja.mbedded.ninjaterm.util.stringUtils.StringUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.fxmisc.richtext.StyledTextArea;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;


/**
 * UI node which presents COM port data to the user (can be either TX, RX, or both).
 * <p>
 * Uses a WebView to implement rich-text formatting
 * functionality (for ANSI escape code colours).
 *
 * @author Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 * @last-modified 2017-10-08
 * @since 2016-11-14
 */
public class ComDataPaneWeb extends StackPane {

    //================================================================================================//
    //====================================== CLASS CONSTANTS =========================================//
    //================================================================================================//

    /**
     * The default buffer size.
     */
    private final int DEFAULT_BUFFER_SIZE = 10000;

    private final boolean SHOW_FIREBUG = false;

    private final Color DEFAULT_COLOR = new Color(0, 1, 0, 1);

    //private final int WEB_VIEW_LOAD_WAIT_TIME_MS = 2000;

    //================================================================================================//
    //=========================================== ENUMS ==============================================//
    //================================================================================================//

    private enum ScrollState {

        /**
         * Scroll pane is always scrolled to the bottom so that new data is displayed.
         * This is the default behaviour.
         */
        FIXED_TO_BOTTOM,

        /**
         * The scroll amount will be modified as new data arrives and old data is removed, so that the user
         * is always looking at the same data, until the data is lost.
         */
        SMART_SCROLL,
    }

    //================================================================================================//
    //=========================================== CLASS FIELDS =======================================//
    //================================================================================================//

    public SimpleBooleanProperty isCaretEnabled = new SimpleBooleanProperty(false);

    public SimpleStringProperty name = new SimpleStringProperty("");

    public final WebView webView;

    public SimpleIntegerProperty bufferSize;

    private SimpleObjectProperty<ScrollState> scrollState = new SimpleObjectProperty<>(ScrollState.FIXED_TO_BOTTOM);

    public SimpleIntegerProperty currNumChars = new SimpleIntegerProperty(0);

    private WebEngine webEngine;

    private double currScrollPos = 0;

    /**
     * This is set by this object when all of the javascript files have been loaded into the WebView.
     * For some reason, including the Javascript in the .html file via a <script> tag does not work, and
     * "undefined is not a function" error can result due to loading time issues (Worker.State.SUCCEEDED did not
     * seem to indicate that the javascript had fully loaded with more than one WebView in the UI).
     */
    private SimpleBooleanProperty safeToRunScripts = new SimpleBooleanProperty(false);

    private Logger logger = LoggerUtils.createLoggerFor(getClass().getName());

    //================================================================================================//
    //========================================== CLASS METHODS =======================================//
    //================================================================================================//

    public ComDataPaneWeb() {

        //==============================================//
        //================ LOGGER SETUP ================//
        //==============================================//

        logger.debug("ComDataPaneWeb() called. Object = " + this);

        //==============================================//
        //============== STYLESHEET SETUP ==============//
        //==============================================//

        getStylesheets().add("ninja/mbedded/ninjaterm/resources/style.css");

        //==============================================//
        //============ STYLED TEXT AREA SETUP ==========//
        //==============================================//

        webView = new WebView();
        webEngine = webView.getEngine();

        getChildren().add(webView);

        // Disable the right-click menu on the WebView. All this menu shows
        // is a "Reload page" button which causes errors if clicked
        webView.setContextMenuEnabled(false);

        final URL mapUrl = this.getClass().getResource("richText.html");

        webEngine.javaScriptEnabledProperty().set(true);

        webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                logger.debug("changed() called with newValue = " + newValue.toString());
                if (newValue != Worker.State.SUCCEEDED) {
                    return;
                }
                logger.debug("WebView has transitioned to State.SUCCEEDED.");

                try {

                    logger.debug("Loading javascript files...");

                    byte[] encoded = IOUtils.toByteArray(getClass().getResourceAsStream("jquery-3.1.1.js"));
                    String code = new String(encoded, Charset.defaultCharset());
                    webEngine.executeScript(code);

                    encoded = IOUtils.toByteArray(getClass().getResourceAsStream("stuff.js"));
                    code = new String(encoded, Charset.defaultCharset());
                    webEngine.executeScript(code);

                    //Thread.sleep(2000);

                    safeToRunScripts.set(true);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        webEngine.load(mapUrl.toExternalForm());

        if (safeToRunScripts.get()) {
            logger.debug("WebView has loaded page and is ready.");

            JSObject window = (JSObject) webEngine.executeScript("window");
            window.setMember("java", this);

            if (SHOW_FIREBUG)
                enableFirebug(webEngine);
            webEngine.setUserStyleSheetLocation(getClass().getResource("style.css").toString());
        } else {
            safeToRunScripts.addListener((observable, oldValue, newValue) ->
            {
                if (newValue) {
                    logger.debug("WebView has loaded page and is ready.");

                    JSObject window = (JSObject) webEngine.executeScript("window");
                    window.setMember("java", this);

                    if (SHOW_FIREBUG)
                        enableFirebug(webEngine);

                    webEngine.setUserStyleSheetLocation(getClass().getResource("style.css").toString());

                    // Call to setup defaults
                    handleScrollStateChanged();


                }
            });
        }


        //==============================================//
        //============== BUFFER SIZE SETUP =============//
        //==============================================//

        bufferSize = new SimpleIntegerProperty(DEFAULT_BUFFER_SIZE);
        bufferSize.addListener((observable, oldValue, newValue) -> {

            logger.debug("bufferSize listener called.");
            // If the buffer size is changed, we may need to trim the data
            // to fit the new size (if smaller)
            trimIfRequired();
        });

        //==============================================//
        //================= SCROLL SETUP ===============//
        //==============================================//

        scrollState.addListener((observable, oldValue, newValue) -> {
            handleScrollStateChanged();
        });

        //==============================================//
        //================== NAME SETUP ================//
        //==============================================//

        name.addListener((observable, oldValue, newValue) -> {
            handleNameChanged();
        });

        //==============================================//
        //================= CARET SETUP ================//
        //==============================================//

        isCaretEnabled.addListener((observable, oldValue, newValue) -> {
            handleIsCaretEnabledChange();
        });

        // This sets the current text color and the current caret color
        appendColor(DEFAULT_COLOR);

        webEngine.setOnStatusChanged(event -> {
            logger.debug("status changed, event.toString() = " + event.toString());
        });


        clearData();
        //==============================================//
        //========= "SAFE TO RUN SCRIPTS" SETUP ========//
        //==============================================//

//        Timer timer = new Timer();
//        timer.schedule(new TimerTask() {
//                           @Override
//                           public void run() {
//                               Platform.runLater(() -> {
//                                   // This is hacky! This is just a simple timeout, and which point the webpage inside
//                                   // WebView should have fully loaded.
//                                   safeToRunScripts.set(true);
//                               });
//                           }
//                       },
//                // 1s seems to be enough to let the web page fully load
//                WEB_VIEW_LOAD_WAIT_TIME_MS);

    }

    private void handleIsCaretEnabledChange() {
        if (isCaretEnabled.get()) {
            logger.debug("Showing caret...");
            runScriptWhenReady("showCaret(true)");
        } else {
            logger.debug("Hiding caret...");
            runScriptWhenReady("showCaret(false)");
        }
    }

    /**
     * Enables Firebug Lite for debugging a webEngine.
     *
     * @param engine the webEngine for which debugging is to be enabled.
     */
    private static void enableFirebug(final WebEngine engine) {
        engine.executeScript("if (!document.getElementById('FirebugLite')){E = document['createElement' + 'NS'] && document.documentElement.namespaceURI;E = E ? document['createElement' + 'NS'](E, 'script') : document['createElement']('script');E['setAttribute']('id', 'FirebugLite');E['setAttribute']('src', 'https://getfirebug.com/' + 'firebug-lite.js' + '#startOpened');E['setAttribute']('FirebugLite', '4');(document['getElementsByTagName']('head')[0] || document['getElementsByTagName']('body')[0]).appendChild(E);E = new Image;E['setAttribute']('src', 'https://getfirebug.com/' + '#startOpened');}");
    }

    private void handleNameChanged() {
        runScriptWhenReady("setName('" + name.get() + "')");
    }


    public void setName(String value) {
        runScriptWhenReady("setName(\"" + value + "\")");
    }

    public void addData(StreamedData data) {

        int currPos = 0;

        // Sort markers
        Collections.sort(data.getMarkers());

        for (Marker marker : data.getMarkers()) {

            // Add all text up to this marker
            appendText(data.getText().substring(currPos, marker.charPos));

            if (marker instanceof ColourMarker) {
                appendColor(((ColourMarker) marker).color);
            } else if (marker instanceof NewLineMarker) {
                appendText("\n");
            } else if (marker instanceof TimeStampMarker) {
                appendTimeStamp(((TimeStampMarker) marker).localDateTime);
            } else
                throw new RuntimeException("Marker sub-type not supported.");

            currPos = marker.charPos;
        }

        // Append all text after last marker
        appendText(data.getText().substring(currPos, data.getText().length()));

        //===================================================//
        //= TRIM START OF DOCUMENT IF EXCEEDS BUFFER LENGTH =//
        //===================================================//


        Integer textHeightBeforeTrim = getTextHeight();

        //logger.debug("textHeightBeforeTrim = " + textHeightBeforeTrim);

        trimIfRequired();

        Integer textHeightAfterTrim = getTextHeight();
        //logger.debug("textHeightAfterTrim = " + textHeightAfterTrim);


        //==============================================//
        //============== SCROLL POSITION ===============//
        //==============================================//

        switch (scrollState.get()) {
            case FIXED_TO_BOTTOM:
                // Scroll to the bottom
                scrollToBottom();
                break;

            case SMART_SCROLL:

                Integer heightChange = textHeightBeforeTrim - textHeightAfterTrim;
                //logger.debug("heightChange = " + heightChange);

                // We need to shift the scroll up by the amount the height changed
                Integer oldScrollTop = getComDataWrapperScrollTop();
                //logger.debug("oldScrollTop = " + oldScrollTop);

                Integer newScrollTop = oldScrollTop - heightChange;
                if (newScrollTop < 0)
                    newScrollTop = 0;
                //logger.debug("newScrollTop = " + newScrollTop);

                setComDataWrapperScrollTop(newScrollTop);

                break;
            default:
                throw new RuntimeException("scrollState not recognised.");
        }

    }

    public void setWrappingEnabled(Boolean value) {
        logger.debug("setWrappingEnabled() called with value = " + value.toString());
        runScriptWhenReady("setWrappingEnabled(" + value.toString() + ")");
    }

    public void setWrappingWidthPx(double width) {
        logger.debug("setWrappingWidthPx() called with width = " + Double.toString(width));
        runScriptWhenReady("setWrappingWidthPx(" + Double.toString(width) + ")");
    }

    private Integer getComDataWrapperScrollTop() {
        return (Integer) webEngine.executeScript("getComDataWrapperScrollTop()");
    }

    private void setComDataWrapperScrollTop(Integer value) {
        runScriptWhenReady("setComDataWrapperScrollTop(" + value + ")");
    }

    public void clearData() {
        logger.debug("clearData() called.");

        // Remove all COM data
        runScriptWhenReady("clearData()");

        // Add new default span (since all existing ones have now
        // been deleted)
        appendColor(DEFAULT_COLOR);

        // Re-show the caret, if it is enabled
        handleIsCaretEnabledChange();

        // Reset scrolling
        setComDataWrapperScrollTop(0);
        currScrollPos = 0;

        // Reset character count
        currNumChars.set(0);


    }

    /**
     * Removes the specified number of characters from the start of the COM data displayed to the user.
     */
    private void trimIfRequired() {

        //logger.debug("trimIfRequired() called.");

        if (currNumChars.get() >= bufferSize.get()) {

            //logger.debug("Trimming data...");

            int numCharsToRemove = currNumChars.get() - bufferSize.get();
            //logger.debug("Need to trimIfRequired display text. currNumChars = " + currNumChars + ", numCharsToRemove = " + numCharsToRemove);

            runScriptWhenReady("trim(" + numCharsToRemove + ")");

            // Update the character count
            currNumChars.set(currNumChars.get() - numCharsToRemove);

            //logger.debug("currNumChars.get() = " + currNumChars.get());
        }
    }

    /**
     * Updates the visibility of the scroll-to-bottom (the down arrow) button.
     * This should be called when <code>scrollState</code> changes.
     */
    private void handleScrollStateChanged() {
        //logger.debug("handleScrollStateChanged() called.");
        switch (scrollState.get()) {
            case FIXED_TO_BOTTOM:
                runScriptWhenReady("showDownArrow(false)");
                break;
            case SMART_SCROLL:
                runScriptWhenReady("showDownArrow(true)");
                break;
            default:
                throw new RuntimeException("scrollState not recognised.");
        }
    }

    /**
     * @param script
     * @warning This is hacky! Relies on {@code safeToRunScripts}, which is set by a simple timeout.
     */
    private void runScriptWhenReady(String script) {
        //logger.debug("runScriptWhenReady() called with script = " + script);

        if (safeToRunScripts.get()) {
            logger.debug("Safe to run JS script immediately.");
            webEngine.executeScript(script);
        } else {
            logger.debug("Scheduling script to run when safeToRunScripts == true...");
            safeToRunScripts.addListener((observable, oldValue, newValue) -> {
                if (newValue) {
                    //logger.debug("Executing script = \"" + script + "\".");
                    webEngine.executeScript(script);
                }
            });

        }
    }

    /**
     * Appends the provided text to the rich text pane inside the WebView.
     * <p>
     * Escapes all Java sequences.
     *
     * @param text
     */
    private void appendText(String text) {

        //logger.debug("appendText() called.");

        // Return if empty string
        // (JS will throw null error)
        if (text.equals(""))
            return;

        // Update the variable that keeps track of the number of displayed
        // chars in the WebView rich text object.
        // Note: Keep track of number of chars BEFORE escaping new lines
        currNumChars.set(currNumChars.get() + text.length());

        // Escape new lines
        //logger.debug("Non-escaped HTML = " + text);
        text = StringEscapeUtils.escapeJava(text);
        //logger.debug("Escaped HTML = " + text);

        String js = "addText(\"" + text + "\")";
        //logger.debug("js = " + js);
//        webEngine.executeScript(js);
        runScriptWhenReady(js);

        //String js2 = "checkCharCount(" + currNumChars.get() + ")";
        //runScriptWhenReady(js2);
    }

    private void appendColor(Color color) {
        String js = "addColor(\"" + StringUtils.toWebColor(color) + "\")";
        //logger.debug("js = " + js);
        //webEngine.executeScript(js);
        runScriptWhenReady(js);

        // Since appendColor() does not add any "text" to the WebView rich text object,
        // we don't update currNumChars here
    }

    private void appendTimeStamp(LocalDateTime localDateTime) {
        // Convert time stamp object into string
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss.SSS: ");
        String timeStamp = localDateTime.format(formatter);

        runScriptWhenReady("appendTimeStamp(\"" + timeStamp + "\")");

        // Update the variable that keeps track of the number of displayed
        // chars in the WebView rich text object.
        currNumChars.set(currNumChars.get() + timeStamp.length());
    }

    private void scrollToBottom() {

        runScriptWhenReady("scrollToBottom()");
    }

    /**
     * This is called from the Javascript in stuff.js.
     * @param text
     */
    public void log(String text) {
        // Since the JavaScript will be calling this, add "JS: " to the front of the
        // messages
        logger.debug("JS: " + text);
    }

    /**
     * Called by Javascript when the user scrolls the COM data up or down
     *
     * @param scrollTop
     */
    @SuppressWarnings("unused")
    public void scrolled(Double scrollTop) {

        //logger.debug("scrolled() called. scrollTop = " + scrollTop);

        if (scrollTop >= currScrollPos) {
            currScrollPos = scrollTop;
            return;
        }

        // If the user scrolled downwards, we don't want to disable auto-scroll,
        // so check and return if so.
        if (scrollState.get() == ScrollState.SMART_SCROLL)
            return;

        //logger.debug("User has scrolled upwards while in SCROLL_TO_BOTTOM mode, disabling SCROLL_TO_BOTTOM...");

        // Since the user has now scrolled upwards (manually), disable the
        // auto-scroll
        scrollState.set(ScrollState.SMART_SCROLL);

        currScrollPos = scrollTop;
    }

    @SuppressWarnings("unused")
    public void downArrowClicked() {
        logger.debug("Down arrow clicked.");

        scrollState.set(ScrollState.FIXED_TO_BOTTOM);

        scrollToBottom();
    }

    private Integer getTextHeight() {
        return (Integer) webEngine.executeScript("getTextHeight()");
    }

    /**
     * Gets called by the Javascript when either the up key is pressed or the mouse wheel is scrolled
     * in the upwards direction (when the WebView has focus).
     * <p>
     * Used for going from FIXED_TO_BOTTOM to the SMART_SCROLL state.
     */
    @SuppressWarnings("unused")
    public void upKeyOrMouseWheelUpOccurred() {

        //logger.debug("upKeyOrMouseWheelUpOccurred() called.");

        // Since the user has now scrolled upwards (manually), disable the
        // auto-scroll
        scrollState.set(ScrollState.SMART_SCROLL);

        //currScrollPos = scrollTop;
    }

    //================================================================================================//
    //========================================== GRAVEYARD ===========================================//
    //================================================================================================//

    //    public void addData(StreamedData streamedData) {
//
//        logger.debug("addData() called with streamedData = " + streamedData);
//
//        //==============================================//
//        //=== ADD ALL TEXT BEFORE FIRST COLOUR CHANGE ==//
//        //==============================================//
//
//
//        // Copy all text before first ColourMarker entry into the first text node
//
//        int indexOfLastCharPlusOne;
//        if (streamedData.getColourMarkers().size() == 0) {
//            indexOfLastCharPlusOne = streamedData.getText().length();
//        } else {
//            indexOfLastCharPlusOne = streamedData.getColourMarkers().get(0).charPos;
//        }
//
//        StringBuilder textToAppend = new StringBuilder(streamedData.getText().substring(0, indexOfLastCharPlusOne));
//
//        // Create new line characters for all new line markers that point to text
//        // shifted above
//        int currNewLineMarkerIndex = 0;
//        for (int i = 0; i < streamedData.getNewLineMarkers().size(); i++) {
//            if (streamedData.getNewLineMarkers().get(currNewLineMarkerIndex).getCharPos() > indexOfLastCharPlusOne)
//                break;
//
//            textToAppend.insert(streamedData.getNewLineMarkers().get(currNewLineMarkerIndex).getCharPos() + i, "\n");
//            currNewLineMarkerIndex++;
//        }
//
//        // If the previous StreamedText object had a colour to apply when the next character was received,
//        // add it now
//        if (colorToApplyToNextChar != null) {
//            appendColor(colorToApplyToNextChar);
//            colorToApplyToNextChar = null;
//        }
//
//        String html;
//        html = textToAppend.toString();
//        //html = html.replace("\n", "<br>");
//
//        appendText(html);
//
//        // Update the number of chars added with what was added to the last existing text node
//        currNumChars.set(currNumChars.get() + textToAppend.toString().length());
//
//
//        // Create new text nodes and copy all text
//        // This loop won't run if there is no elements in the TextColors array
//        //int currIndexToInsertNodeAt = nodeIndexToStartShift;
//        for (int x = 0; x < streamedData.getColourMarkers().size(); x++) {
//            //Text newText = new Text();
//
//            int indexOfFirstCharInNode = streamedData.getColourMarkers().get(x).charPos;
//
//            int indexOfLastCharInNodePlusOne;
//            if (x >= streamedData.getColourMarkers().size() - 1) {
//                indexOfLastCharInNodePlusOne = streamedData.getText().length();
//            } else {
//                indexOfLastCharInNodePlusOne = streamedData.getColourMarkers().get(x + 1).charPos;
//            }
//
//            textToAppend = new StringBuilder(streamedData.getText().substring(indexOfFirstCharInNode, indexOfLastCharInNodePlusOne));
//
//            // Create new line characters for all new line markers that point to text
//            // shifted above
//            int insertionCount = 0;
//            while (true) {
//                if (currNewLineMarkerIndex >= streamedData.getNewLineMarkers().size())
//                    break;
//
//                if (streamedData.getNewLineMarkers().get(currNewLineMarkerIndex).getCharPos() > indexOfLastCharInNodePlusOne)
//                    break;
//
//                textToAppend.insert(
//                        streamedData.getNewLineMarkers().get(currNewLineMarkerIndex).getCharPos() + insertionCount - indexOfFirstCharInNode,
//                        NEW_LINE_CHAR_SEQUENCE_FOR_TEXT_FLOW);
//                currNewLineMarkerIndex++;
//                insertionCount++;
//            }
//
//            //==============================================//
//            //==== ADD TEXT TO STYLEDTEXTAREA AND COLOUR ===//
//            //==============================================//
//
//            appendColor(streamedData.getColourMarkers().get(x).color);
//
//            html = textToAppend.toString();
//            //html = html.replace("\n", "<br>");
//            appendText(html);
//
//            // Update the num. chars added with all the text added to this new Text node
//            currNumChars.set(currNumChars.get() + textToAppend.toString().length());
//        }
//
//        List<ColourMarker> colourMarkers = streamedData.getColourMarkers();
//
//        if(colourMarkers.size() != 0) {
//            if(colourMarkers.get(colourMarkers.size() - 1).charPos == streamedData.getText().length()) {
//
//                colorToApplyToNextChar = colourMarkers.get(colourMarkers.size() - 1).color;
//            }
//        }
//
//
//        // Clear all text and the TextColor list
//        streamedData.clear();
//
//        //checkAllColoursAreInOrder();
//
//        //===================================================//
//        //= TRIM START OF DOCUMENT IF EXCEEDS BUFFER LENGTH =//
//        //===================================================//
//
//
//        Integer textHeightBeforeTrim = getTextHeight();
//
//        logger.debug("textHeightBeforeTrim = " + textHeightBeforeTrim);
//
//        trimIfRequired();
//
//        Integer textHeightAfterTrim = getTextHeight();
//        logger.debug("textHeightAfterTrim = " + textHeightAfterTrim);
//
//
//        //==============================================//
//        //============== SCROLL POSITION ===============//
//        //==============================================//
//
//        switch (scrollState.get()) {
//            case FIXED_TO_BOTTOM:
//                // Scroll to the bottom
//                scrollToBottom();
//                break;
//
//            case SMART_SCROLL:
//
//                Integer heightChange = textHeightBeforeTrim - textHeightAfterTrim;
//                logger.debug("heightChange = " + heightChange);
//
//                // We need to shift the scroll up by the amount the height changed
//                Integer oldScrollTop = getComDataWrapperScrollTop();
//                logger.debug("oldScrollTop = " + oldScrollTop);
//
//                Integer newScrollTop = oldScrollTop - heightChange;
//                if (newScrollTop < 0)
//                    newScrollTop = 0;
//                logger.debug("newScrollTop = " + newScrollTop);
//
//                setComDataWrapperScrollTop(newScrollTop);
//
//                break;
//            default:
//                throw new RuntimeException("scrollState not recognised.");
//        }
//
//    }

}
