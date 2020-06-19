/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import static java.lang.Boolean.TRUE;
import static org.glassfish.grizzly.http.Protocol.HTTP_1_1;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.module.http.internal.listener.grizzly.ResponseDeferringCompletionHandler.FAILURE_WHILE_PROCESSING_HTTP_RESPONSE_BODY;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.collections.MultiHashMap;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Transport;
import org.glassfish.grizzly.filterchain.FilterChain;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.memory.HeapMemoryManager;
import org.glassfish.grizzly.memory.MemoryManager;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
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

public class ResponseDeferringCompletionHandlerOnFailureTestCase extends AbstractMuleTestCase implements OutputHandler
{

    private static final int POOLING_FREQUENCY_MILLIS = 1000;
    private static final int POOLING_TIMEOUT_MILLIS = 20000;

    private FilterChainContext ctx = mock(FilterChainContext.class);
    private FilterChain filterChain = mock(FilterChain.class);
    private Connection connection = mock(Connection.class);
    private Transport transport = mock(Transport.class);
    private MemoryManager memoryManager = new HeapMemoryManager();
    private HttpRequestPacket request = mock(HttpRequestPacket.class);
    private HttpResponse response;
    private Exception exceptionOnFlush;

    private ResponseDeferringCompletionHandler responseDeferringCompletionHandler;

    private AtomicBoolean firstChunkWritten = new AtomicBoolean(false);
    private AtomicBoolean contentWritten = new AtomicBoolean(false);
    private Semaphore stepSync = new Semaphore(0);

    private OutputStream outputStream;


    @Before
    public void setUp() throws Exception
    {
        mockHttpRequestAndResponse();
        responseDeferringCompletionHandler = new ResponseDeferringCompletionHandler(ctx, request, response, mock(ResponseStatusCallback.class));
    }

    @Test
    public void testFlushFailsAndDoNotHangOnFailure() throws IOException, InterruptedException
    {
        responseDeferringCompletionHandler.start();

        waitUntilContentSynchronizer(firstChunkWritten);

        // It fails
        responseDeferringCompletionHandler.failed(new IOException("Broken pipe"));

        // Step sync is released so that the response attempts to flush a chunl again.
        stepSync.release();

        waitUntilContentSynchronizer(contentWritten);
        assertThat(exceptionOnFlush, is(not(nullValue())));
        assertThat(exceptionOnFlush, instanceOf(IOException.class));
        assertThat(exceptionOnFlush.getMessage(), equalTo(FAILURE_WHILE_PROCESSING_HTTP_RESPONSE_BODY));
        assertThat(firstChunkWritten.get(), is(TRUE));
    }

    @Override
    public void write(MuleEvent event, final OutputStream out) throws IOException
    {
        outputStream = out;

        new Thread("Thread that flushes")
        {
            @Override
            public void run()
            {
                try
                {
                    outputStream.write("This is a test string".getBytes());
                    firstChunkWritten.set(true);

                    stepSync.acquire();

                    outputStream.write("This is a second test string".getBytes());
                    outputStream.flush();

                }
                catch (InterruptedException | IOException e)
                {
                    exceptionOnFlush = e;
                }
                contentWritten.set(true);
            }
        }.start();
    }

    private void waitUntilContentSynchronizer(final AtomicBoolean synchronizer)
    {
        new PollingProber(POOLING_TIMEOUT_MILLIS, POOLING_FREQUENCY_MILLIS).check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                return synchronizer.get();
            }

            @Override
            public String describeFailure()
            {
                return "Timeouted waiting for content to be written";
            }
        });
    }

    private void mockHttpRequestAndResponse()
    {
        when(request.getProtocol()).thenReturn(HTTP_1_1);
        MultiHashMap responseHeaders = new MultiHashMap();
        response = new DefaultHttpResponse(new ResponseStatus(), responseHeaders, new OutputHandlerHttpEntity(this));

        when(ctx.getConnection()).thenReturn(connection);
        when(ctx.getFilterChain()).thenReturn(filterChain);
        when(connection.getTransport()).thenReturn(transport);
        when(transport.getMemoryManager()).thenReturn(memoryManager);
    }
}
