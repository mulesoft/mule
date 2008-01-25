/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.email.connectors;

import org.mule.api.MuleEventContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.routing.InboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.routing.inbound.DefaultInboundRouterCollection;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.email.transformers.EmailMessageToString;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import java.util.HashMap;
import java.util.Map;

/**
 * Given an endpoint ({@link #getTestEndpointURI()}) this waits for up to 10 seconds,
 * hoping to receive the message stored in the mail server.  It also runs the unit tests
 * defined way down in {@link org.mule.tck.providers.AbstractConnectorTestCase}.
 */
public abstract class AbstractReceivingMailConnectorTestCase extends AbstractMailConnectorFunctionalTestCase
{
    public static final int POLL_PERIOD_MS = 2000;
    public static final int WAIT_PERIOD_MS = 4 * POLL_PERIOD_MS;

    protected AbstractReceivingMailConnectorTestCase(String protocol, int port)
    {
        super(SEND_INITIAL_EMAIL, protocol, port);
    }

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
                    assertMessageOk(context.getMessage().getOrginalPayload());
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
        ImmutableEndpoint ep = 
            muleContext.getRegistry().lookupEndpointFactory()
                .getInboundEndpoint(getTestEndpointURI());
        InboundRouterCollection inboundRouter = new DefaultInboundRouterCollection();
        inboundRouter.addEndpoint(ep);
        service.setInboundRouter(inboundRouter);
        muleContext.getRegistry().registerService(service);
        //muleContext.applyLifecycle(service);
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
