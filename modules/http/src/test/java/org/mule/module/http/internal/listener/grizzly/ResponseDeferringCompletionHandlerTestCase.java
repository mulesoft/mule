package org.mule.module.http.internal.listener.grizzly;

import static org.apache.http.HttpHeaders.TRANSFER_ENCODING;
import static org.glassfish.grizzly.http.Protocol.HTTP_1_1;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
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
import java.util.concurrent.atomic.AtomicInteger;

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

    /*
    ResponseDeferringCompletionHandler semaphore has to be intercepted in the release method call,
    since the race condition being tested happens in the following situation:

        1 - The contents of the ResponseDeferringCompletionHandler$CompletionOutputStream are flushed.
        This happens for example, when the CH (completion handler) is owned by a DataWeave transformer,
        and it finished writing the current transformation, or whenever a write is going to overfill the buffer.

        2 - When the flush method write to the FilterChainContext, it registers the CH as a completed callback.

        3 - The close method is called on ResponseDeferringCompletionHandler$CompletionOutputStream, and it block
        waiting for the sending semaphore. It also registers a completed callback (B).

        4 - When flush's method completed callback is called (A), it releases the sending semaphore.

        5 - close method continues execution, setting the isDone flag as true.

        6 - flush completed callback (A) continues executing, entering the isDone branch, and calling the doComplete method,
        which notifies the completion of the request downstream in the FilterChain.

        7 - close's completed callback (B) continues executing, entering yet again the isDone branch, calling doComplete,
        and notifies an already finished FilterChain. This leads to a NPE when attempting to reuse an already discarded filter
        chain context.
    */

    private static final String RESPONSE_DEFERRING_CH_SEMAPHORE_FIELD = "sending";
    private static final String TRUE = "true";
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Grizzly requirements mocking
    private FilterChainContext ctx = mock(FilterChainContext.class);
    private FilterChain filterChain = mock(FilterChain.class);
    private Connection connection = mock(Connection.class);
    private Transport transport = mock(Transport.class);
    private MemoryManager memoryManager = new HeapMemoryManager();
    private HttpRequestPacket request = mock(HttpRequestPacket.class);
    private HttpResponse response;

    // class under test
    private ResponseDeferringCompletionHandler responseDeferringCompletionHandler;

    // synchronizers
    private AtomicInteger downstreamNotificationsSent;
    private AtomicInteger contextWritesCompleted;
    private WriteResult mockWriteResult = mock(WriteResult.class);
    private AtomicBoolean contentWritten;
    private CountDownLatch completeAndCloseSync;
    private CountDownLatch flushEnteredWriting;

    private OutputStream outputStream;

    private Semaphore stepSync;


    @Before
    public void setUp() throws Exception
    {
        // downstream notifications counter
        downstreamNotificationsSent = new AtomicInteger(0);

        initializeSynchronizers();

        mockHttpRequestAndResponse();

        // whenever a notification is sent downstream in Grizzly's filter chain, count it
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                logger.error("Downstream notification sent");
                downstreamNotificationsSent.incrementAndGet();
                return null;
            }
        }).when(ctx).notifyDownstream(any(FilterChainEvent.class));

        // Create new completion handler to be tested
        responseDeferringCompletionHandler = new ResponseDeferringCompletionHandler(ctx, request, response, mock(ResponseStatusCallback.class));

        // CH sending semaphore release method has to be intercepted, in order to replicate race condition
        interceptCompletionHandlerSemaphore();
    }


    @Test
    public void testSingleFlushAndCloseGenerateOneDownstreamCompletionEvent() throws IOException, InterruptedException
    {

        logger.warn("Starting responseDeferringCompletionHandler");
        // the CH start method ends up firing up the orchestrator thread, which is
        // in charge of calling flush and close CompletionOutputStream methods
        responseDeferringCompletionHandler.start();

        // some content is written to CompletionOutputStream
        wailUntilContentWritten();

        // ctx.write called from CompletionOutputStream$flush
        doAnswer(flushContextWriteMock()).when(ctx).write(anyObject(), any(CompletionHandler.class));

        // release semaphore to let CompletionOutputStream$flush be called by orchestrator
        logger.warn("Releasing 'flush' sync");
        stepSync.release();

        // Waiting for flush thread to be started
        flushEnteredWriting.await(5, TimeUnit.SECONDS);

        // ctx.write called from CompletionOutputStream$close
        // should block in semaphore, and wait in write for flush's callback to execute
        doAnswer(closeContextWriteMock()).when(ctx).write(anyObject(), any(CompletionHandler.class));

        // release semaphore to let CompletionOutputStream$close be called by orchestrator
        logger.warn("Releasing 'close' sync");
        stepSync.release();

        waitUntilFlushAndCloseExecuted();

        assertThat(downstreamNotificationsSent.get(), is(1));
    }

    // Once the write method is called on the OutputHandler, which in this case
    // is implemented by the TestClass, launch the orchestrator thread, which will
    // trigger each CompletionOutputHanlder method being tested
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
                    // polled flag to make test thread run once content has been written
                    contentWritten.set(true);

                    // wait for signal
                    logger.warn("Acquiring flush sync");
                    stepSync.acquire();
                    // run flush
                    outputStream.flush();

                    // wait for signal
                    logger.warn("Acquiring close sync");
                    stepSync.acquire();
                    // run close
                    outputStream.close();

                }
                catch (InterruptedException | IOException e)
                {
                    // fail fast when exception raised in launched threads
                    failLoggingException(e);
                }
            }
        }.start();
    }

    private void failLoggingException(Exception e)
    {
        fail("Exception raised in " + Thread.currentThread().getName() + ": " + e.toString());
    }

    private Answer<Void> closeContextWriteMock()
    {
        return new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                new Thread("close context writer")
                {
                    @Override
                    public void run ()
                    {
                        // entering FilterChainContext$write from CompletionOutputStream$close => isDone has been set
                        // make flush's callback continue after 'sending' semaphore release
                        completeAndCloseSync.countDown();

                        logger.warn("ctx.write called from close");

                        // Entered ctx.write from close. This means done flag is set in DeferringCompletionHandler
                        responseDeferringCompletionHandler.completed(mockWriteResult);

                        contextWritesCompleted.incrementAndGet();
                    }
                }.start();

                return null;
            }
        };
    }

    private Answer<Void> flushContextWriteMock()
    {
        return new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                new Thread("flush context writer thread")
                {
                    @Override
                    public void run()
                    {
                        logger.warn("ctx.write called from flush");

                        // sync used to make sure mocked FilterChainContext.write delegates
                        // to the creation of this thread, before the mock answer is changed to the
                        // CompletionOutputStream$close one
                        flushEnteredWriting.countDown();

                        // Final context write completion call
                        responseDeferringCompletionHandler.completed(mockWriteResult);

                        contextWritesCompleted.incrementAndGet();
                    }
                }.start();
                return null;
            }
        };
    }


    private void waitUntilFlushAndCloseExecuted()
    {
        new PollingProber(10000, 1000).check(new Probe()
        {

            private int expectedWrites = 2;

            @Override
            public boolean isSatisfied()
            {
                return contextWritesCompleted.get() == expectedWrites;
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

    private void interceptCompletionHandlerSemaphore() throws NoSuchFieldException, IllegalAccessException
    {
        Field sendingSemaphore = responseDeferringCompletionHandler.getClass().getDeclaredField(RESPONSE_DEFERRING_CH_SEMAPHORE_FIELD);
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
                    // fail fast when exception raised in launched threads
                    failLoggingException(e);
                }
            }
        });
    }

    private void initializeSynchronizers()
    {
        flushEnteredWriting = new CountDownLatch(1);
        stepSync = new Semaphore(0);
        contentWritten = new AtomicBoolean(false);
        completeAndCloseSync = new CountDownLatch(1);
        contextWritesCompleted = new AtomicInteger(0);
    }

    private void mockHttpRequestAndResponse()
    {
        when(request.getProtocol()).thenReturn(HTTP_1_1);

        MultiHashMap responseHeaders = new MultiHashMap();
        responseHeaders.put(TRANSFER_ENCODING, TRUE);
        response = new DefaultHttpResponse(new ResponseStatus(), responseHeaders, new OutputHandlerHttpEntity(this));

        when(ctx.getConnection()).thenReturn(connection);
        when(ctx.getFilterChain()).thenReturn(filterChain);
        when(connection.getTransport()).thenReturn(transport);
        when(transport.getMemoryManager()).thenReturn(memoryManager);
    }
}
