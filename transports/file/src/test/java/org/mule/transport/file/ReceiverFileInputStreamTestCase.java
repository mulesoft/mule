/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mule.util.UUID;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ReceiverFileInputStreamTestCase
{
    @ClassRule
    public static TemporaryFolder inputTemporaryFolder = new TemporaryFolder();

    @ClassRule
    public static TemporaryFolder moveToTemporaryFolder = new TemporaryFolder();

    @Mock
    InputStreamCloseListener listener;

    File input;

    File output;

    @Before
    public void prepare() throws IOException
    {
        input = getTestFile();
        output = getTestFile();
    }

    @Test
    public void testStreamingError() throws IOException
    {
        closeReceiver(input, output, true, listener, true);
        assertThat(input.exists(), is(true));
        verify(listener).fileClose(input);
    }

    @Test
    public void testNonStreamingError() throws IOException
    {
        closeReceiver(input, output, true, listener, false);
        assertThat(input.exists(), is(false));
        verify(listener).fileClose(input);
    }

    @Test
    public void testMultipleStreamingError() throws IOException
    {
        closeReceiver(input, output, true, listener, true);
        closeReceiver(getTestFile(), getTestFile(), true, listener, true);
        closeReceiver(getTestFile(), getTestFile(), true, listener, true);
        assertThat(input.exists(), is(true));
        verify(listener).fileClose(input);
    }

    @Test
    public void testMultipleNonStreamingError() throws IOException
    {
        closeReceiver(input, output, true, listener, false);
        closeReceiver(getTestFile(), getTestFile(), true, listener, false);
        closeReceiver(getTestFile(), getTestFile(), true, listener, false);
        assertThat(input.exists(), is(false));
        verify(listener).fileClose(input);
    }

    private File getTestFile() throws IOException
    {
        return inputTemporaryFolder.newFile(UUID.getUUID());
    }

    private void closeReceiver(File inputFirst, File output, boolean deleteOnClose, InputStreamCloseListener listener, boolean streamingError) throws IOException
    {
        ReceiverFileInputStream receiverStream = new ReceiverFileInputStream(inputFirst, true, output, listener);
        receiverStream.setStreamProcessingError(streamingError);
        receiverStream.close();
    }

}
