/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import org.mule.api.MuleEventContext;
import org.mule.api.context.notification.ExceptionNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.context.notification.ExceptionNotification;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.tck.functional.CounterCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class JmsRedeliveryTestCase extends FunctionalTestCase
{
    private final int timeout = getTimeoutSecs() * 1000 / 4;
    private static final String DESTINATION = "jms://in";

    protected String getConfigResources()
    {
        return "jms-redelivery.xml";
    }

    public void testRedelivery() throws Exception
    {
        MuleClient client = new MuleClient();
        // required if broker is not restarted with the test - it tries to deliver those messages to the client
        // purge the queue
        while (client.request(DESTINATION, 1000) != null)
        {
            logger.warn("Destination " + DESTINATION + " isn't empty, draining it");
        }

        FunctionalTestComponent ftc = getFunctionalTestComponent("Bouncer");

        // whether a MessageRedeliverdException has been fired
        final Latch mrexFired = new Latch();
        muleContext.registerListener(new ExceptionNotificationListener()
        {

            public void onNotification(ServerNotification notification)
            {
                ExceptionNotification n = (ExceptionNotification) notification;
                if (n.getException() instanceof MessageRedeliveredException)
                {
                    mrexFired.countDown();
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

        client.dispatch(DESTINATION, "test", null);

        mrexFired.await(timeout, TimeUnit.MILLISECONDS);
        assertEquals("MessageRedeliveredException never fired.", 0, mrexFired.getCount());
        assertEquals("Wrong number of delivery attempts", 4, callback.getCallbackCount());
    }

}