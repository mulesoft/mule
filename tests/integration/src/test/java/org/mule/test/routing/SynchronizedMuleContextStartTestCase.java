/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.construct.Flow;
import org.mule.endpoint.DefaultEndpointFactory;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.util.endpoint.InboundEndpointWrapper;
import org.mule.util.concurrent.Latch;

import org.junit.Test;

public class SynchronizedMuleContextStartTestCase extends FunctionalTestCase
{

    private static volatile int processedMessageCounter = 0;
    private static Latch waitMessageInProgress = new Latch();

    public SynchronizedMuleContextStartTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "synchronized-mule-context-start-config.xml";
    }

    @Test
    public void waitsForStartedMuleContextBeforeAttemptingToSendMessageToEndpoint() throws Exception
    {
        prePopulateObjectStore();

        muleContext.start();

        Prober prober = new PollingProber(RECEIVE_TIMEOUT, 50);

        prober.check(new Probe()
        {
            public boolean isSatisfied()
            {
                return processedMessageCounter == 1;
            }

            public String describeFailure()
            {
                return "Did not wait for mule context started before attempting to process event";
            }
        });
    }

    private void prePopulateObjectStore() throws ObjectStoreException
    {
        ObjectStore<MuleEvent> objectStore = muleContext.getRegistry().lookupObject("objectStore");

        DefaultMuleMessage testMessage = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        Flow clientFlow = muleContext.getRegistry().get("flow2");
        DefaultMuleEvent testMuleEvent = new DefaultMuleEvent(testMessage, MessageExchangePattern.REQUEST_RESPONSE, clientFlow);
        objectStore.store(testMuleEvent.getId(), testMuleEvent);
    }

    public static class ProcessedMessageCounter
    {

        public String count(String value)
        {
            processedMessageCounter++;
            return value;
        }
    }

    public static class UnblockEndpointStart
    {

        public void unclockEndpoint(String value)
        {
            waitMessageInProgress.release();
        }
    }

    public static class DelayedStartEndpointFactory extends DefaultEndpointFactory
    {

        public InboundEndpoint getInboundEndpoint(EndpointBuilder builder) throws MuleException
        {
            InboundEndpoint endpoint = builder.buildInboundEndpoint();

            if (endpoint.getName().equals("endpoint.vm.flow2"))
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
                // Need to wait some time so the outbound endpoint from the first flow attempts to send the message.
                // Waiting on the latch ensures that the until successful from the first flow is processing the event.
                // Need the sleep to give us some time to let the dispatcher to try to send the event and ther eis no way
                // to do that without a Thread.sleep
                waitMessageInProgress.await();
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }

            super.start();
        }
    }
}
