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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.UUID;
import org.mule.util.concurrent.Latch;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ReceiverFileInputStreamTestCase extends AbstractMuleTestCase
{
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    InputStreamCloseListener listener;

    @Mock
    InputStreamCloseListener latchedListener;

    Latch latchedListenerLatch = new Latch();

    File inputFileSpy;

    File outputFileSpy;

    @Before
    public void prepare() throws IOException
    {
        inputFileSpy = Mockito.spy(createTestFile());
        outputFileSpy = Mockito.spy(createTestFile());

        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws InterruptedException {
                latchedListenerLatch.await();
                return null;
            }
        }).when(latchedListener).fileClose(any(File.class));
    }

    @Test
    public void testStreamingError() throws IOException
    {
        ReceiverFileInputStream receiver = createReceiver(inputFileSpy, outputFileSpy, true, listener, true);
        receiver.close();

        assertThat(inputFileSpy.exists(), is(true));
        verify(listener).fileClose(inputFileSpy);
        verify(inputFileSpy, never()).renameTo(any(File.class));
        verify(inputFileSpy, never()).delete();
    }

    @Test
    public void testNonStreamingErrorWithDelete() throws IOException
    {
        ReceiverFileInputStream receiver = createReceiver(inputFileSpy, null, true, listener, false);
        receiver.close();

        assertThat(inputFileSpy.exists(), is(false));
        verify(listener).fileClose(inputFileSpy);
        verify(inputFileSpy, never()).renameTo(any(File.class));
        verify(inputFileSpy).delete();
    }

    @Test
    public void testNonStreamingErrorWithoutDelete() throws IOException
    {
        ReceiverFileInputStream receiver = createReceiver(inputFileSpy, outputFileSpy, false, listener, false);
        receiver.close();

        assertThat(inputFileSpy.exists(), is(false));
        verify(listener).fileClose(inputFileSpy);
        verify(inputFileSpy).renameTo(any(File.class));
        verify(inputFileSpy, never()).delete();
    }

    @Test
    public void testMultipleThreadedStreamingError() throws IOException, InterruptedException {
        ReceiverFileInputStream receiver = createReceiver(inputFileSpy, outputFileSpy, true, latchedListener, true);
        receiverCloseThreaded(receiver);
        receiverCloseThreaded(receiver);
        latchedListenerLatch.release();
        receiver.close();

        assertThat(inputFileSpy.exists(), is(true));
        verify(latchedListener).fileClose(any(File.class));
    }

    @Test
    public void testMultipleThreadedNonStreamingError() throws IOException
    {
        ReceiverFileInputStream receiver = createReceiver(inputFileSpy, outputFileSpy, true, latchedListener, false);
        receiverCloseThreaded(receiver);
        receiverCloseThreaded(receiver);
        latchedListenerLatch.release();
        receiver.close();

        assertThat(inputFileSpy.exists(), is(false));
        verify(latchedListener).fileClose(inputFileSpy);
    }

    private File createTestFile() throws IOException
    {
        return temporaryFolder.newFile(UUID.getUUID());
    }

    private ReceiverFileInputStream createReceiver(File input, File output, boolean deleteOnClose, InputStreamCloseListener listener, boolean streamingError) throws IOException
    {
        ReceiverFileInputStream receiverStream = new ReceiverFileInputStream(input, true, output, listener);
        receiverStream.setStreamProcessingError(streamingError);
        return receiverStream;
    }

    private void receiverCloseThreaded(final ReceiverFileInputStream receiver) throws IOException
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    receiver.close();
                }
                catch (IOException e)
                {
                    fail();
                }
            }
        }).start();
    }

}
