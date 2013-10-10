/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.connectors;

import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.source.CompositeMessageSource;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.email.transformers.EmailMessageToString;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Given an endpoint ({@link #getTestEndpointURI()}) this waits for up to 10 seconds,
 * hoping to receive the message stored in the mail server.  It also runs the unit tests
 * defined way down in {@link org.mule.transport.AbstractConnectorTestCase}.
 */
public abstract class AbstractReceivingMailConnectorTestCase extends AbstractMailConnectorFunctionalTestCase
{
    public static final int POLL_PERIOD_MS = 2000;
    public static final int WAIT_PERIOD_MS = 4 * POLL_PERIOD_MS;

    protected AbstractReceivingMailConnectorTestCase(String protocol)
    {
        super(SEND_INITIAL_EMAIL, protocol);
    }

    @Test
    public void testReceiver() throws Exception
    {
        final CountDownLatch countDown = new CountDownLatch(1);

        HashMap props = new HashMap();
        props.put("eventCallback", new EventCallback()
        {
            public synchronized void eventReceived(MuleEventContext context, Object component)
            {
                try
                {
                    logger.debug("woot - event received");
                    logger.debug("context: " + context);
                    logger.debug("component: " + component);
                    assertMessageOk(context.getMessage().getOriginalPayload());
                    countDown.countDown();
                }
                catch (Exception e)
                {
                    // counter will not be incremented
                    logger.error(e.getMessage(), e);
                }
            }
        });

        Service service = MuleTestUtils.getTestService(uniqueName("testComponent"), FunctionalTestComponent.class, props, muleContext, /*initialize*/false);
        EndpointBuilder eb = muleContext.getEndpointFactory().getEndpointBuilder(getTestEndpointURI());
        eb.setDisableTransportTransformer(true);
        InboundEndpoint ep = eb.buildInboundEndpoint();
        ((CompositeMessageSource) service.getMessageSource()).addSource(ep);
        muleContext.getRegistry().registerService(service);
        if (!muleContext.isStarted())
        {
            muleContext.start();
        }

        logger.debug("waiting for count down");
        assertTrue(countDown.await(WAIT_PERIOD_MS, TimeUnit.MILLISECONDS));
    }

    protected static Map newEmailToStringServiceOverrides()
    {
        Map serviceOverrides = new HashMap();
        serviceOverrides.put(MuleProperties.CONNECTOR_INBOUND_TRANSFORMER,
                EmailMessageToString.class.getName());
        return serviceOverrides;
    }

}
