/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class ReceiverFileInputStreamTestCase extends AbstractMuleTestCase
{
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    InputStreamCloseListener listener;

    File inputFileSpy;

    File outputFileSpy;

    @Before
    public void prepare() throws IOException
    {
        inputFileSpy = Mockito.spy(createTestFile());
        outputFileSpy = Mockito.spy(createTestFile());
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
    public void testMultipleThreadedStreamingError() throws IOException, InterruptedException, ExecutionException {
        ReceiverFileInputStream receiver = createReceiver(inputFileSpy, outputFileSpy, true, listener, true);
        receiverCloseThreaded(receiver, 2);
        receiver.close();

        assertThat(inputFileSpy.exists(), is(true));
        verify(listener).fileClose(any(File.class));
    }

    @Test
    public void testMultipleThreadedNonStreamingError() throws IOException, InterruptedException, ExecutionException {
        ReceiverFileInputStream receiver = createReceiver(inputFileSpy, outputFileSpy, true, listener, false);
        receiverCloseThreaded(receiver, 2);

        assertThat(inputFileSpy.exists(), is(false));
        verify(listener).fileClose(inputFileSpy);
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

    private void receiverCloseThreaded(final ReceiverFileInputStream receiver, int numberThreads) throws IOException, ExecutionException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(numberThreads);
        ExecutorService pool = Executors.newFixedThreadPool(numberThreads);

        Callable<Void> receiverCloseRunnable = new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                latch.countDown();
                latch.await();
                receiver.close();
                return null;
            }
        };

        try
        {
            List<Future> futures = new ArrayList<Future>(numberThreads);
            for(int i=0; i<numberThreads; i++)
            {
                futures.add(pool.submit(receiverCloseRunnable));
            }
            for(Future future: futures)
            {
                future.get();
            }
        }
        finally
        {
            pool.shutdown();
        }
    }

}
