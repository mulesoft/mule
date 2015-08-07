/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.util.concurrent.Latch;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequestMaxConnectionsTestCase extends AbstractHttpRequestTestCase
{
    private static final int SMALL_RESPONSE_TIMEOUT = 1;

    private Latch messageArrived = new Latch();
    private Latch messageHold = new Latch();

    @Override
    protected String getConfigFile()
    {
        return "http-request-max-connections-config.xml";
    }

    @Test
    public void maxConnections() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("limitedConnections");
        Thread t1 = processAsynchronously(flow);
        messageArrived.await();
        try
        {
            // Process an event with a very small timeout, this should fail because there is already one
            // active connection, and maxConnections=1
            flow.process(createEvent(SMALL_RESPONSE_TIMEOUT));
            fail("Max connections should be reached.");
        }
        catch(MessagingException e)
        {
            // Expected
            assertThat(e.getCause(), instanceOf(IOException.class));
        }
        messageHold.release();
        t1.join();
    }

    private Thread processAsynchronously(final Flow flow)
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    flow.process(createEvent(RECEIVE_TIMEOUT));
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.start();
        return thread;
    }

    private MuleEvent createEvent(int timeout) throws Exception
    {
        MuleEvent event = getTestEvent(TEST_MESSAGE);
        event.setTimeout(timeout);
        return event;
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        super.handleRequest(baseRequest, request, response);
        messageArrived.release();
        try
        {
            messageHold.await();
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
    }

}
