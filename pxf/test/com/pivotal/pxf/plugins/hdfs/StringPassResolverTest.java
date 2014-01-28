package com.pivotal.pxf.plugins.hdfs;

import com.pivotal.pxf.api.OneField;
import com.pivotal.pxf.api.OneRow;
import com.pivotal.pxf.api.OutputFormat;
import com.pivotal.pxf.api.utilities.InputData;
import com.pivotal.pxf.core.BridgeInputBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InputData.class, LogFactory.class})
public class StringPassResolverTest {
    InputData inputData;
    Log Log;

    @Test
    /*
     * Test the setFields method: small \n terminated input
	 */
    public void testSetFields() throws Exception {
        StringPassResolver resolver = buildResolver(false);

        byte[] data = new byte[]{(int) 'a', (int) 'b', (int) 'c', (int) 'd', (int) '\n',
                (int) 'n', (int) 'o', (int) '\n'};

        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(data));
        BridgeInputBuilder inputBuilder = new BridgeInputBuilder(inputData);
        List<OneField> record = inputBuilder.makeInput(inputStream);

        OneRow oneRow = resolver.setFields(record);
        verifyOneRow(oneRow, Arrays.copyOfRange(data, 0, 5));

        record = inputBuilder.makeInput(inputStream);
        oneRow = resolver.setFields(record);
        verifyOneRow(oneRow, Arrays.copyOfRange(data, 5, 8));
    }

    @Test
    /*
     * Test the setFields method: input > buffer size, \n terminated
	 */
    public void testSetFieldsBigArray() throws Exception {

        StringPassResolver resolver = buildResolver(false);

        byte[] bigArray = new byte[2000];
        for (int i = 0; i < 1999; ++i) {
            bigArray[i] = (byte) (i % 10 + 30);
        }
        bigArray[1999] = (byte) '\n';

        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(bigArray));
        BridgeInputBuilder inputBuilder = new BridgeInputBuilder(inputData);
        List<OneField> record = inputBuilder.makeInput(inputStream);

        OneRow oneRow = resolver.setFields(record);

        verifyOneRow(oneRow, bigArray);
    }

    @Test
    /*
     * Test the setFields method: input > buffer size, no \n
	 */
    public void testSetFieldsBigArrayNoNewLine() throws Exception {

        StringPassResolver resolver = buildResolver(false);

        byte[] bigArray = new byte[2000];
        for (int i = 0; i < 2000; ++i) {
            bigArray[i] = (byte) (i % 10 + 60);
        }

        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(bigArray));
        BridgeInputBuilder inputBuilder = new BridgeInputBuilder(inputData);
        List<OneField> record = inputBuilder.makeInput(inputStream);


        OneRow oneRow = resolver.setFields(record);

        verifyOneRow(oneRow, bigArray);

        //verifyWarning();
    }

    @Test
    /*
	 * Test the setFields method: empty stream (returns -1)
	 */
    public void testSetFieldsEmptyStream() throws Exception {

        StringPassResolver resolver = buildResolver(false);

        byte[] empty = new byte[0];

        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(empty));
        BridgeInputBuilder inputBuilder = new BridgeInputBuilder(inputData);
        List<OneField> record = inputBuilder.makeInput(inputStream);

        OneRow oneRow = resolver.setFields(record);

        assertNull(oneRow);
    }
	
	/*
	 * helpers functions
	 */

    private StringPassResolver buildResolver(boolean hasWarn)
            throws Exception {
        // prepare log for warning
        if (hasWarn) {
            PowerMockito.mockStatic(LogFactory.class);
            Log = mock(Log.class);
            when(LogFactory.getLog(StringPassResolver.class)).thenReturn(Log);
        }

        inputData = mock(InputData.class);
        PowerMockito.when(inputData.outputFormat()).thenReturn(OutputFormat.TEXT);

        return new StringPassResolver(inputData);
    }

    private void verifyOneRow(OneRow oneRow, byte[] expected) {
        assertNull(oneRow.getKey());
        byte[] bytes = (byte[]) oneRow.getData();
        byte[] result = Arrays.copyOfRange(bytes, 0, bytes.length);
        assertEquals(result.length, expected.length);
        assertTrue(Arrays.equals(result, expected));
    }

    private void verifyWarning() {
        Mockito.verify(Log).warn("Stream ended without line break");
    }
}
