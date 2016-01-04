/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;


import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequestTimeoutTestCase extends AbstractHttpRequestTestCase
{

    private static int TEST_TIMEOUT = 2000;

    private Latch serverLatch = new Latch();

    @Override
    protected String getConfigFile()
    {
        return "http-request-timeout-config.xml";
    }

    @Test
    public void throwsExceptionWhenRequesterTimeoutIsExceeded() throws Exception
    {
        assertTimeout("requestFlow", 1, -1);
    }

    @Test
    public void throwsExceptionWhenEventTimeoutIsExceeded() throws Exception
    {
        assertTimeout("requestFlowNoTimeout", -1, 1);
    }

    @Test
    public void requesterTimeoutOverridesEventTimeout() throws Exception
    {
        assertTimeout("requestFlow", 1, TEST_TIMEOUT * 2);
    }

    private void assertTimeout(final String flowName, final int responseTimeoutRequester, final int responseTimeoutEvent) throws Exception
    {
        final Latch requestTimeoutLatch = new Latch();

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    Flow flow = (Flow) getFlowConstruct(flowName);
                    MuleEvent event = getTestEvent(TEST_MESSAGE);
                    event.setTimeout(responseTimeoutEvent);
                    event.setFlowVariable("timeout", responseTimeoutRequester);

                    try
                    {
                        flow.process(event);
                    }
                    catch (MessagingException e)
                    {
                        assertThat(e.getCauseException(), instanceOf(TimeoutException.class));
                        requestTimeoutLatch.release();
                    }
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };

        thread.start();

        // Wait for the request to timeout (the thread sending the request will release the latch.
        assertTrue(requestTimeoutLatch.await(TEST_TIMEOUT, TimeUnit.MILLISECONDS));
        thread.join();

        // Release the server latch so that the server thread can finish.
        serverLatch.release();
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        try
        {
            // Block until the end of the test in order to make the request timeout.
            serverLatch.await();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
