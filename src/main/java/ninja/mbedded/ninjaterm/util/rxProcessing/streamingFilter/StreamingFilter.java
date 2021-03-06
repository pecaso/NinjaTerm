package ninja.mbedded.ninjaterm.util.rxProcessing.streamingFilter;

import ninja.mbedded.ninjaterm.util.debugging.Debugging;
import ninja.mbedded.ninjaterm.util.loggerUtils.LoggerUtils;
import ninja.mbedded.ninjaterm.util.rxProcessing.streamedData.StreamedData;
import org.slf4j.Logger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Class contains a static method for shifting a provided number of characters from one input
 * <code>{@link StreamedData}</code> object to another output <code>{@link StreamedData}</code>
 * object.</p>
 *
 * @author Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 * @since 2016-09-28
 * @last-modified 2016-10-03
 */
public class StreamingFilter {

    private boolean releaseTextOnCurrLine = false;

    private String filterPattern = "";

    Pattern regexPattern;

    private Logger logger = LoggerUtils.createLoggerFor(getClass().getName());

    public void setFilterPattern(String filterPattern) {

        this.filterPattern = filterPattern;
        regexPattern = Pattern.compile(filterPattern);

        // Reset filter engine
        releaseTextOnCurrLine = false;

    }

    /**
     * This method provides a filtering function based on an incoming stream of data.
     *
     */
    public void parse(
            StreamedData inputStreamedData,
            StreamedData outputStreamedData) {

        logger.debug(getClass().getSimpleName() + ".parse() called with:");
        logger.debug("inputStreamedData { " + Debugging.convertNonPrintable(inputStreamedData.toString()) + "}.");
        logger.debug("outputStreamedData { " + Debugging.convertNonPrintable(outputStreamedData.toString()) + "}.");

        if(filterPattern.equals("")) {
            logger.debug("Filter text empty. Not performing any filtering.");

            // Shift all input to output
            outputStreamedData.shiftDataIn(inputStreamedData, inputStreamedData.getText().length(), StreamedData.MarkerBehaviour.NOT_FILTERING);
            return;
        }

        if(inputStreamedData.getText().equals("")) {
            logger.debug("No filtering to perform. Returning...");
            return;
        }

        // Search for new line characters
        //String lines[] = inputStreamedData.getText().split("(?<=[\\n])");
        String lines[] = inputStreamedData.splitTextAtNewLines();

        for (String line : lines) {

            // Check to see if we can release all text on this line without even bothering
            // to check for a match. This will occur if a match has already occurred on this line.
            if(releaseTextOnCurrLine) {
                logger.debug("releaseTextOnCurrLine = true. Releasing text " + Debugging.convertNonPrintable(line));
                outputStreamedData.shiftDataIn(inputStreamedData, line.length(), StreamedData.MarkerBehaviour.FILTERING);

                /*if(hasNewLineChar(line)) {
                    releaseTextOnCurrLine = false;
                }*/

                if(line != lines[lines.length - 1]) {
                    releaseTextOnCurrLine = false;
                }

                // Jump to next iteration of for loop
                logger.debug("Going to next iteration of loop.");
                continue;
            }


            Matcher matcher = regexPattern.matcher(line);

            if (matcher.find()) {
                // Match in line found!
                logger.debug("Match in line found. Line = " + Debugging.convertNonPrintable(line));

                // We can release all text/nodes up to the end of this line
                int numCharsToRelease = line.length();
                logger.debug("numCharsToRelease = " + numCharsToRelease);
                outputStreamedData.shiftDataIn(inputStreamedData, numCharsToRelease, StreamedData.MarkerBehaviour.FILTERING);

                // Check to see if this is the last line. If so, set the releaseTextOnCurrLine to true
                // so that next time this function is called, any other text which is also on this line
                // will be released without question

                //if(line == lines[lines.length - 1] && !hasNewLineChar(line)) {
                if(line == lines[lines.length - 1]) {
                    releaseTextOnCurrLine = true;
                }
            } else {
                // No match found on this line. If this line is completed, then we know there can never be a match,
                // and it can be deleted from the heldStreamedText
                logger.debug("No match found on line = " + Debugging.convertNonPrintable(line));

                //if(hasNewLineChar(line)) {
                if(line != lines[lines.length - 1]) {
                    logger.debug("Deleting line.");
                    inputStreamedData.removeCharsFromStart(line.length(), true);
                }

            }
        } // for (String line : lines)

        logger.debug(getClass().getSimpleName() + ".parse() finished. Variables are now:");
        logger.debug("inputStreamedData { " + Debugging.convertNonPrintable(inputStreamedData.toString()) + "}.");
        logger.debug("outputStreamedData { " + Debugging.convertNonPrintable(outputStreamedData.toString()) + "}.");

    }

    /**
     * This method can be used to determine if a string contains a new line character.
     * @param line
     * @return
     */
    /*public static boolean hasNewLineChar(String line) {
        Pattern pattern = Pattern.compile("\n");
        Matcher matcher = pattern.matcher(line);

        if(matcher.find())
            return true;
        else
            return false;
    }*/

}
