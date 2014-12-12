/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.listener;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.util.concurrent.Latch;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class HttpListenerWorkerThreadingProfileTestCase extends FunctionalTestCase
{

    private static final int MAX_THREADS_ACTIVE = 3;
    private static CountDownLatch maxActiveNumberOfRequestExecutedLatch = new CountDownLatch(MAX_THREADS_ACTIVE);
    @Rule
    public DynamicPort listenPort = new DynamicPort("port");
    @Rule
    public DynamicPort listenPort2 = new DynamicPort("port2");
    @Rule
    public SystemProperty maxThreadsActive = new SystemProperty("max.threads.active", String.valueOf(MAX_THREADS_ACTIVE));
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Override
    protected String getConfigFile()
    {
        return "http-listener-worker-threading-profile-config.xml";
    }

    @Test
    public void useMaxThreadsActiveThreadingProfile() throws Exception
    {
        sendRequestUntilNoMoreWorkers();
        expectedException.expect(NoHttpResponseException.class);
        try
        {
            Request.Get(String.format("http://localhost:%s", listenPort.getNumber())).execute();
        }
        finally
        {
            WaitMessageProcessor.waitingLatch.release();
        }
    }

    @Test
    public void hitDifferentRequestConfigAndRun() throws Exception
    {
        sendRequestUntilNoMoreWorkers();
        try
        {
            final Response response = Request.Post(String.format("http://localhost:%s", listenPort2.getNumber())).bodyByteArray(TEST_MESSAGE.getBytes()).connectTimeout(100).socketTimeout(100).execute();
            final HttpResponse httpResponse = response.returnResponse();
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(200));
            assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_MESSAGE));
        }
        finally
        {
            WaitMessageProcessor.waitingLatch.release();
        }
    }

    private void sendRequestUntilNoMoreWorkers() throws InterruptedException
    {
        for (int i = 0; i < Integer.valueOf(maxThreadsActive.getValue()); i++)
        {
            executeRequestInAnotherThread();
        }
        if (!maxActiveNumberOfRequestExecutedLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message processor wasn't executed the number of times required.");
        }
    }

    private void executeRequestInAnotherThread()
    {
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    Request.Get(String.format("http://localhost:%s", listenPort.getNumber())).connectTimeout(RECEIVE_TIMEOUT).execute();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

    public static class WaitMessageProcessor implements MessageProcessor
    {
        private static Latch waitingLatch = new Latch();
        private static AtomicInteger numberOfRequest = new AtomicInteger();

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                maxActiveNumberOfRequestExecutedLatch.countDown();
                numberOfRequest.incrementAndGet();
                if (numberOfRequest.get() <= MAX_THREADS_ACTIVE)
                {
                    waitingLatch.await();
                }
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            return event;
        }
    }
}
