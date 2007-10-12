/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.email.connectors;

import org.mule.config.MuleProperties;
import org.mule.providers.email.transformers.EmailMessageToString;
import org.mule.routing.inbound.InboundRouterCollection;
import org.mule.tck.MuleTestUtils;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.routing.UMOInboundRouterCollection;

import java.util.HashMap;
import java.util.Map;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

/**
 * Given an endpoint ({@link #getTestEndpointURI()}) this waits for up to 10 seconds,
 * hoping to receive the message stored in the mail server.  It also runs the unit tests
 * defined way down in {@link org.mule.tck.providers.AbstractConnectorTestCase}.
 */
public abstract class AbstractReceivingMailConnectorTestCase extends AbstractMailConnectorFunctionalTestCase
{
    
    public static final int POLL_PERIOD_MS = 2000;
    public static final int WAIT_PERIOD_MS = 3 * POLL_PERIOD_MS;

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
            public synchronized void eventReceived(UMOEventContext context, Object component)
            {
                try 
                {
                    logger.debug("woot - event received");
                    logger.debug("context: " + context);
                    logger.debug("component: " + component);
                    assertMessageOk(context.getMessage().getPayload());
                    countDown.countDown();
                } 
                catch (Exception e) 
                {
                    // counter will not be incremented
                    logger.error(e.getMessage(), e);
                }
            }
        });

        UMOComponent component = MuleTestUtils.getTestComponent(uniqueName("testComponent"), FunctionalTestComponent.class, props, managementContext, /*initialize*/false);
        UMOImmutableEndpoint ep = 
            managementContext.getRegistry().lookupEndpointFactory()
                .createInboundEndpoint(getTestEndpointURI(), managementContext);
        ep.initialise();
        UMOInboundRouterCollection inboundRouter = new InboundRouterCollection();
        inboundRouter.addEndpoint(ep);
        component.setInboundRouter(inboundRouter);
        managementContext.getRegistry().registerComponent(component, managementContext);
        managementContext.applyLifecycle(component);
        if (!managementContext.isStarted())
        {
            managementContext.start();
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
