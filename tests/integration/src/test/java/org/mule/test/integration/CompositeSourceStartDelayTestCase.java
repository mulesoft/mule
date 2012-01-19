/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration;

import static org.junit.Assert.assertEquals;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.lifecycle.Startable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.TransformerException;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.transformer.AbstractTransformer;

import org.junit.Test;

public class CompositeSourceStartDelayTestCase extends FunctionalTestCase
{

    public static volatile boolean awakeMessageSource;

    public CompositeSourceStartDelayTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "composite-source-start-delay-config.xml";
    }

    @Test
    public void testProcessMessageWhenAnSourceIsNotStartedYet() throws Exception
    {
        Thread thread = new Thread(new Runnable()
        {

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

        waitUntilEndpointIsStarted("testInEndpoint");


        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://testIn", "TEST", null);
        assertEquals("TEST received", response.getPayloadAsString());
    }

    private void waitUntilEndpointIsStarted(final String endpointName)
    {
        Prober prober = new PollingProber(30000, 50);
        prober.check(new Probe()
        {

            public boolean isSatisfied()
            {
                DefaultInboundEndpoint endpoint = (DefaultInboundEndpoint) muleContext.getRegistry().lookupObject(endpointName);
                return endpoint.getConnector().isStarted();
            }

            public String describeFailure()
            {
                return "Endpoint was not started";
            }
        });
    }

    public static class AwakeSourceMessageProcessor implements MessageProcessor
    {

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            awakeMessageSource = true;

            return event;
        }
    }

    public static class StuckTransformer extends AbstractTransformer implements Startable
    {

        @Override
        protected Object doTransform(Object src, String enc) throws TransformerException
        {
            return null;
        }

        public void start() throws MuleException
        {
            while (!awakeMessageSource)
            {
                try
                {
                    Thread.sleep(10);
                }
                catch (InterruptedException e)
                {
                    // Nothing to do
                }
            }
        }
    }
}


