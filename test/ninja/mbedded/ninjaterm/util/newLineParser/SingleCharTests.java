package ninja.mbedded.ninjaterm.util.newLineParser;

import javafx.scene.paint.Color;
import ninja.mbedded.ninjaterm.util.streamedText.StreamedText;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the <code>NewLineParser</code> class.
 *
 * @author          Geoffrey Hunter <gbmhunter@gmail.com> (www.mbedded.ninja)
 * @since           2016-10-15
 * @last-modified   2016-10-15
 */
public class SingleCharTests {

    private StreamedText inputStreamedText;
    private StreamedText outputStreamedText;

    private NewLineParser newLineParser;

    @Before
    public void setUp() throws Exception {
        inputStreamedText = new StreamedText();
        outputStreamedText = new StreamedText();

        newLineParser = new NewLineParser("\n");
    }

    @Test
    public void noNewLineTest() throws Exception {

        inputStreamedText.append("1234");

        newLineParser.parse(inputStreamedText, outputStreamedText);

        assertEquals("", inputStreamedText.getText());
        assertEquals(0, inputStreamedText.getTextColours().size());
        assertEquals(0, inputStreamedText.getNewLineIndicies().size());

        assertEquals("1234", outputStreamedText.getText());
        assertEquals(0, outputStreamedText.getTextColours().size());
        assertEquals(0, outputStreamedText.getNewLineIndicies().size());
    }

    @Test
    public void oneNewLineTest() throws Exception {

        inputStreamedText.append("123\n456");

        newLineParser.parse(inputStreamedText, outputStreamedText);

        assertEquals("", inputStreamedText.getText());
        assertEquals(0, inputStreamedText.getTextColours().size());
        assertEquals(0, inputStreamedText.getNewLineIndicies().size());

        assertEquals("123\n456", outputStreamedText.getText());
        assertEquals(0, outputStreamedText.getTextColours().size());
        assertEquals(1, outputStreamedText.getNewLineIndicies().size());
        assertEquals(3, outputStreamedText.getNewLineIndicies().get(0).intValue());
    }

    @Test
    public void twoNewLinesTest() throws Exception {

        inputStreamedText.append("123\n456\n789");

        newLineParser.parse(inputStreamedText, outputStreamedText);

        assertEquals("", inputStreamedText.getText());
        assertEquals(0, inputStreamedText.getTextColours().size());
        assertEquals(0, inputStreamedText.getNewLineIndicies().size());

        assertEquals("123\n456\n789", outputStreamedText.getText());
        assertEquals(0, outputStreamedText.getTextColours().size());
        assertEquals(2, outputStreamedText.getNewLineIndicies().size());
        assertEquals(3, outputStreamedText.getNewLineIndicies().get(0).intValue());
        assertEquals(7, outputStreamedText.getNewLineIndicies().get(1).intValue());
    }

    @Test
    public void onlyANewLineTest() throws Exception {

        inputStreamedText.append("\n");

        newLineParser.parse(inputStreamedText, outputStreamedText);

        assertEquals("", inputStreamedText.getText());
        assertEquals(0, inputStreamedText.getTextColours().size());
        assertEquals(0, inputStreamedText.getNewLineIndicies().size());

        assertEquals("\n", outputStreamedText.getText());
        assertEquals(0, outputStreamedText.getTextColours().size());
        assertEquals(1, outputStreamedText.getNewLineIndicies().size());
        assertEquals(0, outputStreamedText.getNewLineIndicies().get(0).intValue());
    }

    @Test
    public void twoNewLinesInARowTest() throws Exception {

        inputStreamedText.append("\n\n");

        newLineParser.parse(inputStreamedText, outputStreamedText);

        assertEquals("", inputStreamedText.getText());
        assertEquals(0, inputStreamedText.getTextColours().size());
        assertEquals(0, inputStreamedText.getNewLineIndicies().size());

        assertEquals("\n\n", outputStreamedText.getText());
        assertEquals(0, outputStreamedText.getTextColours().size());
        assertEquals(2, outputStreamedText.getNewLineIndicies().size());
        assertEquals(0, outputStreamedText.getNewLineIndicies().get(0).intValue());
        assertEquals(1, outputStreamedText.getNewLineIndicies().get(1).intValue());
    }

}