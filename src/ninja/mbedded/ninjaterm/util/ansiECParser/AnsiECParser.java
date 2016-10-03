package ninja.mbedded.ninjaterm.util.ansiECParser;

import javafx.scene.paint.Color;
import ninja.mbedded.ninjaterm.util.debugging.Debugging;
import ninja.mbedded.ninjaterm.util.streamedText.StreamedText;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class that decodes ANSI escape sequences.
 *
 * @author          Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 * @since           2016-09-26
 * @last-modified   2016-09-30
 */
public class AnsiECParser {

    Map<String, Color> codeToNormalColourMap = new HashMap<>();
    Map<String, Color> codeToBoldColourMap = new HashMap<>();

    private Pattern p;

    /**
     * Partial matches and the end of provided input strings to <code>parse()</code> are
     * stored in this variable for the next time <code>parse() is called.</code>
     */
    private String withheldTextWithPartialMatch = "";

    public AnsiECParser() {
        // Populate the map with data
        codeToNormalColourMap.put("30", Color.rgb(0, 0, 0));
        codeToNormalColourMap.put("31", Color.rgb(170, 0, 0));
        codeToNormalColourMap.put("32", Color.rgb(0, 170, 0));
        codeToNormalColourMap.put("33", Color.rgb(170, 85, 0));
        codeToNormalColourMap.put("34", Color.rgb(0, 0, 170));
        codeToNormalColourMap.put("35", Color.rgb(170, 0, 170));
        codeToNormalColourMap.put("36", Color.rgb(0, 170, 170));
        codeToNormalColourMap.put("37", Color.rgb(170, 170, 170));

        codeToBoldColourMap.put("30", Color.rgb(85, 85, 85));
        codeToBoldColourMap.put("31", Color.rgb(255, 85, 85));
        codeToBoldColourMap.put("32", Color.rgb(85, 255, 85));
        codeToBoldColourMap.put("33", Color.rgb(255, 255, 85));
        codeToBoldColourMap.put("34", Color.rgb(85, 85, 225));
        codeToBoldColourMap.put("35", Color.rgb(255, 85, 255));
        codeToBoldColourMap.put("36", Color.rgb(85, 255, 255));
        codeToBoldColourMap.put("37", Color.rgb(255, 255, 255));

        // This pattern matches an ANSI escape code. It matches an arbitrary number of
        // numbers after the "[ESC][", separated by a ";" and then prefixed by a "m".
        p = Pattern.compile("\u001B\\[[;\\d]*m");
    }

    /**
     *
     * Runs the ANSI escape code parser on the input streaming text, and produces and output StreamedText object.
     *
     * @param inputString           The input string which can contain display text and ANSI escape codes.
     * @param outputStreamedText    Contains streamed text that has been release from this parser. The internal appendText
     *                              variable will contain text which can be added to the previous node (i.e. no colour change),
     *                              and new nodes with text of different colours.
     */
    public void parse(String inputString, StreamedText outputStreamedText) {

//        System.out.println(getClass().getSimpleName() + ".parse() called with inputString = " + Debugging.convertNonPrintable(inputString));

//        if(inputString.charAt(inputString.length() - 1) == 'm') {
//            int bogus = 0;
//        }

        //int numCharsAdded = 0;

        // Prepend withheld text onto the end of the input string
        String withheldCharsAndInputString = withheldTextWithPartialMatch + inputString;
        withheldTextWithPartialMatch = "";

        Matcher m = p.matcher(withheldCharsAndInputString);

        //String remainingInput = "";
        int currPositionInString = 0;

        //m.reset();
        while (m.find(currPositionInString)) {
//            System.out.println("find() is true. m.start() = " + m.start() + ", m.end() = " + m.end() + ".");

            // Everything up to the first matched character can be added to the last existing text node
            String preText = withheldCharsAndInputString.substring(currPositionInString, m.start());
            outputStreamedText.append(preText);

            //numCharsAdded += preText.length();

            // Now extract the code
            String ansiEscapeCode = withheldCharsAndInputString.substring(m.start(), m.end());
            //System.out.println("ANSI esc seq = " + toHexString(ansiEscapeCode));

            // Save the remaining text to process
            //remainingInput = inputString.substring(m.end(), inputString.length());

            // Extract the numbers from the escape code
            String[] numbers = extractNumbersAsArray(ansiEscapeCode);

            Map<String, Color> correctMapToUse;
            if(numbers.length == 1) {
                correctMapToUse = codeToNormalColourMap;
            } else if(numbers.length == 2 && numbers[1].equals("1")) {
                correctMapToUse = codeToBoldColourMap;
            } else {
                // ANSI escape sequence is not supported. Remove it from input and continue
                //throw new RuntimeException("Numbers not recognised!");
                currPositionInString = m.end();
                continue;
            }

            // Get the colour associated with this code
            Color color = correctMapToUse.get(numbers[0]);

            if(color == null) {
                System.out.println("Escape sequence was not supported!");
                // The number in the escape sequence was not recognised. Update the current position in input string
                // to skip over this escape sequence, and continue to next iteration of loop.
                currPositionInString = m.end();
                continue;
            }

//            System.out.println("Valid escape seqeunce found.");

            // Create new Text object with this new color, and add to the text nodes
            outputStreamedText.setColorToBeInsertedOnNextChar(color);

            currPositionInString = m.end();

        }

        int firstCharAfterLastFullMatch = currPositionInString;

        // Look for index of partial match
        int startIndexOfPartialMatch = -1;
        while((startIndexOfPartialMatch == -1) && (currPositionInString <= (withheldCharsAndInputString.length() - 1))) {

            m = p.matcher(withheldCharsAndInputString.substring(currPositionInString));
            m.matches();
            if(m.hitEnd()) {
                startIndexOfPartialMatch = currPositionInString;
            }

            // Remove first character from input and try again
           currPositionInString++;
        }

        // There might be remaining input after the last ANSI escpe code has been processed.
        // This can all be put in the last text node, which should be by now set up correctly.
        if (startIndexOfPartialMatch == -1) {

            String charsToAppend = withheldCharsAndInputString.substring(firstCharAfterLastFullMatch);
//            System.out.println("No partial match found. charsToAppend = " + Debugging.convertNonPrintable(charsToAppend));
            //addTextToLastNode(outputStreamedText, charsToAppend);
            outputStreamedText.append(charsToAppend);
            //numCharsAdded += charsToAppend.length();
        } else if(startIndexOfPartialMatch > firstCharAfterLastFullMatch) {

            String charsToAppend = withheldCharsAndInputString.substring(firstCharAfterLastFullMatch, startIndexOfPartialMatch);
//            System.out.println("Partial match found. charsToAppend = " + Debugging.convertNonPrintable(charsToAppend));
            //addTextToLastNode(outputStreamedText, charsToAppend);
            outputStreamedText.append(charsToAppend);
            //numCharsAdded += charsToAppend.length();
        }

        // Finally, save the partial match for the next run
        if(startIndexOfPartialMatch != -1) {
            withheldTextWithPartialMatch = withheldCharsAndInputString.substring(startIndexOfPartialMatch);
//            System.out.println("Withholding text. withheldTextWithPartialMatch = " + Debugging.convertNonPrintable(withheldTextWithPartialMatch));
        }

    }

    private String[] extractNumbersAsArray(String ansiEscapeCode) {

        // Input should be in the form
        // (ESC)[xx;xx;...xxm
        // We want to extract the x's

        // Trim of the (ESC) and [ chars from the start, and the m from the end
        String trimmedString = ansiEscapeCode.substring(2, ansiEscapeCode.length() - 1);

        //System.out.println("trimmedString = " + trimmedString);

        String[] numbers = trimmedString.split(";");

        //System.out.println("numbers = " + toString(numbers));

        return numbers;
    }

}
