/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.junit.runners.Parameterized;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.context.notification.ExceptionNotification;
import org.mule.message.ExceptionMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transport.jms.redelivery.MessageRedeliveredException;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

public class JmsRedeliveryTestCase extends AbstractServiceAndFlowTestCase
{

    private final int timeout = getTestTimeoutSecs() * 1000 / 4;
    private static final String DESTINATION = "jms://in";
    private static final int MAX_REDELIVERY = 3;

    public JmsRedeliveryTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE, "jms-redelivery-service.xml"},
                {ConfigVariant.FLOW, "jms-redelivery-flow.xml"}});
    }

    @Test
    public void testRedelivery() throws Exception
    {
        MuleClient client = muleContext.getClient();
        // required if broker is not restarted with the test - it tries to deliver those messages to the client
        // purge the queue
        while (client.request(DESTINATION, 1000) != null)
        {
            logger.warn("Destination " + DESTINATION + " isn't empty, draining it");
        }

        FunctionalTestComponent ftc = getFunctionalTestComponent("Bouncer");

        // whether a MessageRedeliverdException has been fired
        final Latch messageRedeliveryExceptionFired = new Latch();
        muleContext.registerListener(new ExceptionNotificationListener<ExceptionNotification>()
        {
            public void onNotification(ExceptionNotification notification)
            {
                if (notification.getException() instanceof MessageRedeliveredException)
                {
                    messageRedeliveryExceptionFired.countDown();
                    // Test for MULE-4630
                    assertEquals(DESTINATION, ((MessageRedeliveredException) notification.getException()).getEndpoint().getEndpointURI().toString());
                    assertEquals(MAX_REDELIVERY, ((MessageRedeliveredException) notification.getException()).getMaxRedelivery());
                    assertTrue(((MessageRedeliveredException) notification.getException()).getMuleMessage().getPayload() instanceof javax.jms.Message);
                }
            }
        });

        // enhance the counter callback to count, then throw an exception
        final CounterCallback callback = new CounterCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object Component) throws Exception
            {
                final int count = incCallbackCount();
                logger.info("Message Delivery Count is: " + count); 
                throw new FunctionalTestException();
            }
        };
        ftc.setEventCallback(callback);

        client.dispatch(DESTINATION, TEST_MESSAGE, null);

        Thread.sleep(2000);
        if (!messageRedeliveryExceptionFired.await(timeout, TimeUnit.MILLISECONDS))
        {
            fail("Exception from FunctionalTestComponent was not triggered three times");
        }
        assertEquals("MessageRedeliveredException never fired.", 0, messageRedeliveryExceptionFired.getCount());
        assertEquals("Wrong number of delivery attempts", MAX_REDELIVERY + 1, callback.getCallbackCount());

        MuleMessage dl = client.request("jms://dead.letter", 1000);
        assertNotNull(dl);
        assertTrue(dl.getPayload() instanceof ExceptionMessage);
        ExceptionMessage em = (ExceptionMessage) dl.getPayload();
        assertNotNull(em.getException());
        assertTrue(em.getException() instanceof MessageRedeliveredException);
    }
}
