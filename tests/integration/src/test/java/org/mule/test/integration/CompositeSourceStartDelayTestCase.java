/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.functional.util.endpoint.InboundEndpointWrapper;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.endpoint.EndpointBuilder;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.lifecycle.StartException;
import org.mule.runtime.core.endpoint.DefaultEndpointFactory;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.util.concurrent.CountDownLatch;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Rule;
import org.junit.Test;

public class CompositeSourceStartDelayTestCase extends FunctionalTestCase
{

    public static final CountDownLatch startLatch = new CountDownLatch(1);

    @Rule
    public DynamicPort httpPort = new DynamicPort("httpPort");

    public CompositeSourceStartDelayTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "composite-source-start-delay-config.xml";
    }

    @Test
    public void testProcessMessageWhenAnSourceIsNotStartedYet() throws Exception
    {
        try
        {
            asynchronousMuleContextStart();

            PollingProber prober = new PollingProber(RECEIVE_TIMEOUT, 50);
            prober.check(new ProcessMessageProbe());
        }
        finally
        {
            startLatch.countDown();
        }
    }

    private void asynchronousMuleContextStart()
    {
        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    muleContext.start();
                }
                catch (MuleException e)
                {
                    // Nothing to do
                }
            }
        });

        thread.start();
    }

    private class ProcessMessageProbe implements Probe
    {

        private final HttpClient httpClient = new HttpClient();

        @Override
        public boolean isSatisfied()
        {
            GetMethod method = new GetMethod("http://localhost:" + httpPort.getValue());

            try
            {
                int statusCode = httpClient.executeMethod(method);
                String response = method.getResponseBodyAsString();

                return 200 == statusCode && "Processed".equals(response);
            }
            catch (Exception e)
            {
                return false;
            }
        }

        @Override
        public String describeFailure()
        {
            return "Unable to process message when composite source was not completely started";
        }
    }

    public static class DelayedStartEndpointFactory extends DefaultEndpointFactory
    {

        @Override
        public InboundEndpoint getInboundEndpoint(EndpointBuilder builder) throws MuleException
        {
            InboundEndpoint endpoint = builder.buildInboundEndpoint();

            if (endpoint.getName().equals("sleepingTestIn"))
            {
                InboundEndpointWrapper wrappedEndpoint = new DelayedStartInboundEndpointWrapper(endpoint);

                return wrappedEndpoint;
            }
            else
            {
                return endpoint;
            }
        }
    }

    public static class DelayedStartInboundEndpointWrapper extends InboundEndpointWrapper
    {

        public DelayedStartInboundEndpointWrapper(InboundEndpoint delegate)
        {
            super(delegate);
        }

        @Override
        public void start() throws MuleException
        {
            try
            {
                startLatch.await();
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new StartException(e, this);
            }

            super.start();
        }
    }
}


