package org.mule.module.http.internal.listener.grizzly;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.api.MuleEvent;
import org.mule.api.transport.OutputHandler;
import org.mule.module.http.internal.domain.OutputHandlerHttpEntity;
import org.mule.module.http.internal.domain.response.DefaultHttpResponse;
import org.mule.module.http.internal.domain.response.HttpResponse;
import org.mule.module.http.internal.domain.response.ResponseStatus;
import org.mule.module.http.internal.listener.async.ResponseStatusCallback;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.grizzly.http.Protocol;
import org.slf4j.Logger;

import org.apache.commons.collections.MultiHashMap;
import org.glassfish.grizzly.CompletionHandler;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.WriteResult;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.FilterChainEvent;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.memory.MemoryManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

public class ResponseDeferringCompletionHandlerTestCase extends AbstractMuleTestCase implements OutputHandler
{

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private FilterChainContext ctx = mock(FilterChainContext.class);
    private FilterChain filterChain = mock(FilterChain.class);
    private Connection connection = mock(Connection.class);
    private Transport transport = mock(Transport.class);
    private MemoryManager memoryManager = new HeapMemoryManager();

    private HttpRequestPacket request = mock(HttpRequestPacket.class);
    private Integer downstreamNotificationsSent;
    private HttpResponse response;
    private CountDownLatch syncLatch;
    private CountDownLatch flushEnteredWriting;
    private ResponseDeferringCompletionHandler responseDeferringCompletionHandler;
    private WriteResult mockWriteResult = mock(WriteResult.class);
    private AtomicBoolean contentWritten;
    private CountDownLatch completeAndCloseSync;

    private OutputStream outputStream;

    private Semaphore stepSync;

    private int contextWritesCompleted;

    @Before
    public void setUp() throws Exception
    {
        downstreamNotificationsSent = 0;
        syncLatch = new CountDownLatch(1);
        flushEnteredWriting = new CountDownLatch(1);
        stepSync = new Semaphore(0);
        contentWritten = new AtomicBoolean(false);
        completeAndCloseSync = new CountDownLatch(1);
        contextWritesCompleted = 0;

        mockHttpRequestAndResponse();

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                logger.error("Downstream notification sent");
                downstreamNotificationsSent++;
                return null;
            }
        }).when(ctx).notifyDownstream(any(FilterChainEvent.class));

        responseDeferringCompletionHandler = new ResponseDeferringCompletionHandler(ctx, request, response, mock(ResponseStatusCallback.class));

        Field sendingSemaphore = responseDeferringCompletionHandler.getClass().getDeclaredField("sending");
        sendingSemaphore.setAccessible(true);
        sendingSemaphore.set(responseDeferringCompletionHandler, new Semaphore(1) {
            @Override
            public void release()
            {
                super.release();
                try
                {
                    completeAndCloseSync.await();
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        });

    }

    private void mockHttpRequestAndResponse()
    {
        when(request.getProtocol()).thenReturn(Protocol.HTTP_1_1);

        MultiHashMap responseHeaders = new MultiHashMap();
        responseHeaders.put("Transfer-Encoding", "true");
        response = new DefaultHttpResponse(new ResponseStatus(), responseHeaders, new OutputHandlerHttpEntity(this));

        when(ctx.getConnection()).thenReturn(connection);
        when(ctx.getFilterChain()).thenReturn(filterChain);
        when(connection.getTransport()).thenReturn(transport);
        when(transport.getMemoryManager()).thenReturn(memoryManager);
    }

    @Test
    public void testSingleFlushAndCloseGenerateOneDownstreamCompletionEvent() throws IOException, InterruptedException
    {

        logger.warn("Starting responseDeferringCompletionHandler");
        responseDeferringCompletionHandler.start();

        wailUntilContentWritten();

        // ctx.write called from CompletionOutputStream$flush
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                new Thread("flush context writer thread")
                {
                    @Override
                    public void run()
                    {
                        logger.warn(Thread.currentThread().getName() + " !! ctx.write called from flush");

                        flushEnteredWriting.countDown();

                        syncLatch.countDown();
                        responseDeferringCompletionHandler.completed(mockWriteResult);

                        contextWritesCompleted++;
                    }
                }.start();
                return null;
            }
        }).when(ctx).write(anyObject(), any(CompletionHandler.class));

        // class flush on outputstream
        logger.warn("Releasing 'flush' sync");
        stepSync.release();

        // Waiting for flush thread to be started
        flushEnteredWriting.await(5, TimeUnit.SECONDS);

        // ctx.write called from CompletionOutputStream$close
        // should block in semaphore, and wait in write for flush's callback to execute
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                new Thread("close context writer")
                {
                    @Override
                    public void run ()
                    {
                        completeAndCloseSync.countDown();

                        logger.warn(Thread.currentThread().getName() + " !! ctx.write called from close");

                        // Entered ctx.write from close. This means done flag is set in DeferringCompletionHandler
                        responseDeferringCompletionHandler.completed(mockWriteResult);

                        contextWritesCompleted++;
                    }
                }.start();

                return null;
            }
        }).when(ctx).write(anyObject(), any(CompletionHandler.class));

        logger.warn("Releasing 'close' sync");
        stepSync.release();

        waitUntilFlushAndCloseExecuted();

        assertThat(downstreamNotificationsSent, is(1));
    }

    private void waitUntilFlushAndCloseExecuted()
    {
        new PollingProber(10000, 1000).check(new Probe()
        {

            private int expectedWrites = 2;

            @Override
            public boolean isSatisfied()
            {
                return contextWritesCompleted == expectedWrites;
            }

            @Override
            public String describeFailure()
            {
                return "Not all context writes completed. It actually is " + contextWritesCompleted + " while the expected is " + expectedWrites;
            }
        });
    }

    private void wailUntilContentWritten()
    {
        new PollingProber(10000, 1000).check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return contentWritten.get();
            }

            @Override
            public String describeFailure()
            {
                return "Timeouted waiting for content to be written";
            }
        });

        logger.warn("Content written");
    }


    @Override
    public void write(MuleEvent event, final OutputStream out) throws IOException
    {

        // Save deferred output stream
        outputStream = out;

        // Orchestrator thread
        // Should perform the following method calls
        // 1 - flush
        // 2 - close
        new Thread("Orchestrator thread")
        {
            @Override
            public void run()
            {
                try
                {
                    // To make CompletionOutputStream's buffer not empty
                    logger.warn("About to write stream contents");

                    outputStream.write("This is a test string".getBytes());
                    contentWritten.set(true);

                    // wait for signal
                    logger.warn("Acquiring flush sync");
                    stepSync.acquire();
                    // run flush
                    outputStream.flush();

                    // wait for signal
                    logger.warn("Acquiring close sync");
                    stepSync.acquire();
                    // run flush
                    syncLatch.await(5, TimeUnit.SECONDS);
                    outputStream.close();

                }
                catch (InterruptedException | IOException e)
                {
                    logger.warn("Exception raised in " + Thread.currentThread().getName() + ": " + e.toString());
                    e.printStackTrace();
                }
            }
        }.start();

    }
}
