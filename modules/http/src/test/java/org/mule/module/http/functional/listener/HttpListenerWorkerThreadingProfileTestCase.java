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
import static org.mule.module.http.api.HttpConstants.HttpStatus.ACCEPTED;
import static org.mule.module.http.api.HttpConstants.HttpStatus.DROPPED;
import static org.mule.module.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.module.http.api.HttpConstants.HttpStatus.SERVICE_UNAVAILABLE;
import static org.mule.module.http.internal.listener.DefaultHttpListenerConfig.DEFAULT_MAX_THREADS;
import static org.apache.http.impl.nio.client.HttpAsyncClients.createDefault;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.message.BasicHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mule.api.MuleEventContext;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.util.concurrent.Latch;

public class HttpListenerWorkerThreadingProfileTestCase extends FunctionalTestCase
{

    private static final int CUSTOM_MAX_THREADS_ACTIVE = 3;
    private static final int HTTP_CLIENT_MAX_CONNECTIONS = 200;

    @Rule
    public DynamicPort listenPort1 = new DynamicPort("port1");
    @Rule
    public DynamicPort listenPort2 = new DynamicPort("port2");
    @Rule
    public DynamicPort listenPort3 = new DynamicPort("port3");
    @Rule
    public DynamicPort listenPort4 = new DynamicPort("port4");
    @Rule
    public DynamicPort listenPort5 = new DynamicPort("port5");
    @Rule
    public SystemProperty maxThreadsActive = new SystemProperty("max.threads.active", String.valueOf(CUSTOM_MAX_THREADS_ACTIVE));

    private CountDownLatch maxActiveNumberOfRequestExecutedLatch;
    private Latch waitingLatch = new Latch();
    private AtomicInteger numberOfRequest = new AtomicInteger();
    private Executor httpClientExecutor;

    @Override
    protected String getConfigFile()
    {
        return "http-listener-worker-threading-profile-config.xml";
    }

    @Before
    public void setup()
    {
        // Need to configure the maximum number of connections of HttpClient, because the default is less than
        // the default number of workers in the HTTP listener.
        PoolingHttpClientConnectionManager mgr = new PoolingHttpClientConnectionManager();
        mgr.setDefaultMaxPerRoute(HTTP_CLIENT_MAX_CONNECTIONS);
        mgr.setMaxTotal(HTTP_CLIENT_MAX_CONNECTIONS);
        httpClientExecutor = Executor.newInstance(HttpClientBuilder.create().setConnectionManager(mgr).build());
    }

    @Test
    public void useMaxThreadsActiveThreadingProfile() throws Exception
    {
        String url = String.format("http://localhost:%s", listenPort1.getNumber());
        assertMaxThreadsActive("maxActiveThreadsConfigFlow", url, CUSTOM_MAX_THREADS_ACTIVE, SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Test
    public void useDefaultMaxThreadsActiveThreadingProfile() throws Exception
    {
        String url = String.format("http://localhost:%s", listenPort2.getNumber());
        assertMaxThreadsActive("defaultMaxActiveThreadsConfigFlow", url, DEFAULT_MAX_THREADS, SERVICE_UNAVAILABLE.getStatusCode());
    }

    @Test
    public void useDefaultMaxThreadsActiveThreadingProfileDiscardPolicy() throws Exception
    {
        String url = String.format("http://localhost:%s", listenPort4.getNumber());
        assertMaxThreadsActive("defaultMaxActiveThreadsThreadingProfileDiscardPolicyFlow", url, DEFAULT_MAX_THREADS, ACCEPTED.getStatusCode());
    }

    @Test
    public void useDefaultMaxThreadsActiveThreadingProfileDiscardOldestPolicy() throws Exception
    {
        String url = String.format("http://localhost:%s", listenPort5.getNumber());
        maxActiveNumberOfRequestExecutedLatch = new CountDownLatch(DEFAULT_MAX_THREADS);

        sendRequestUntilNoMoreWorkers("defaultMaxActiveThreadsThreadingProfileDiscardOldestPolicyFlow", url, DEFAULT_MAX_THREADS);

        try
        {
            CloseableHttpAsyncClient httpclient = createDefault();
            httpclient.start();
            final HttpGet request = new HttpGet(url);
            final TestFutureCallback  futureCallback = new TestFutureCallback();
            // The first request will be buffered
            httpclient.execute(request, futureCallback);
            // This is to guarantee that the first message is the one to be enqueued as pending
            // As this is internal to grizzly and to the executor pool
            // There is no condition to verify that the request has already been
            // sent. That is why a Thread.sleep is used.
            Thread.sleep(1000); 
            // After this, the first request must be rejected
            httpclient.execute(request, null);

            new PollingProber(60000, 1000).check(new Probe()
            {
                @Override
                public boolean isSatisfied()
                {
                    try
                    {
                        if (futureCallback.getStatusCode() == -1)
                        {
                            return false;
                        }

                        return futureCallback.getStatusCode() == ACCEPTED.getStatusCode() && futureCallback.getReasonPhrase().equals(DROPPED.getReasonPhrase());
                    }
                    catch (Exception e)
                    {
                        return false;
                    }
                }

                @Override
                public String describeFailure()
                {
                    return "Request was not rejected with 204 code";
                }
            });
        }
        finally
        {
            waitingLatch.release();
        }

    }

    private void assertMaxThreadsActive(String flowName, String url, int maxThreadsActive, int expectedHttpStatus) throws Exception
    {
        maxActiveNumberOfRequestExecutedLatch = new CountDownLatch(maxThreadsActive);

        sendRequestUntilNoMoreWorkers(flowName, url, maxThreadsActive);

        try
        {
            Response response = httpClientExecutor.execute(Request.Get(url));
            int statusCode = response.returnResponse().getStatusLine().getStatusCode();
            assertThat(statusCode, is(expectedHttpStatus));
        }
        finally
        {
            waitingLatch.release();
        }
    }

    @Test
    public void hitDifferentRequestConfigAndRun() throws Exception
    {
        String url = String.format("http://localhost:%s", listenPort1.getNumber());
        sendRequestUntilNoMoreWorkers("maxActiveThreadsConfigFlow", url, CUSTOM_MAX_THREADS_ACTIVE);
        try
        {
            url = String.format("http://localhost:%s", listenPort3.getNumber());
            final Response response = httpClientExecutor.execute(Request.Post(url).bodyByteArray(TEST_MESSAGE.getBytes()).connectTimeout(100).socketTimeout(100));
            final HttpResponse httpResponse = response.returnResponse();
            assertThat(httpResponse.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
            assertThat(IOUtils.toString(httpResponse.getEntity().getContent()), is(TEST_MESSAGE));
        }
        finally
        {
            waitingLatch.release();
        }
    }

    private void sendRequestUntilNoMoreWorkers(String flowName, String url, int maxThreadsActive) throws Exception
    {
        configureTestComponent(flowName, maxThreadsActive);
        maxActiveNumberOfRequestExecutedLatch = new CountDownLatch(maxThreadsActive);

        for (int i = 0; i < maxThreadsActive; i++)
        {
            executeRequestInAnotherThread(url);
        }
        if (!maxActiveNumberOfRequestExecutedLatch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS))
        {
            fail("message processor wasn't executed the number of times required.");
        }
    }

    private void executeRequestInAnotherThread(final String url)
    {
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    httpClientExecutor.execute(Request.Get(url).connectTimeout(RECEIVE_TIMEOUT));
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }.start();
    }

    /**
     * Configures a test component in a specific flow to block until a specific number of concurrent requests are
     * reached.
     */
    private void configureTestComponent(String flowName, final int maxThreadsActive) throws Exception
    {
        getFunctionalTestComponent(flowName).setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                try
                {
                    maxActiveNumberOfRequestExecutedLatch.countDown();
                    numberOfRequest.incrementAndGet();
                    if (numberOfRequest.get() <= maxThreadsActive)
                    {
                        waitingLatch.await();
                    }
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static class TestFutureCallback implements FutureCallback<HttpResponse>
    {

        private int statusCode = -1;
        private String reasonPhrase;

        @Override
        public void cancelled()
        {
        }

        @Override
        public void completed(HttpResponse response)
        {
            this.statusCode = response.getStatusLine().getStatusCode();
            this.reasonPhrase = response.getStatusLine().getReasonPhrase();
        }

        @Override
        public void failed(Exception ex)
        {
        }

        public int getStatusCode()
        {
            return statusCode;
        }

        public String getReasonPhrase()
        {
            return reasonPhrase;
        }
    }
}
