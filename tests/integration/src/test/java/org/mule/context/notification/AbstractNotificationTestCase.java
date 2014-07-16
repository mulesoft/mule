/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.junit.Assert.fail;
import org.mule.api.context.notification.ServerNotification;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.Iterator;

import org.junit.Test;

/**
 * Tests must define a "notificationLogger" listener
 */
public abstract class AbstractNotificationTestCase extends AbstractServiceAndFlowTestCase
{
    private NotificationLogger notificationLogger;

    public AbstractNotificationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public final void testNotifications() throws Exception
    {
        doTest();
        notificationLogger = (NotificationLogger) muleContext.getRegistry()
            .lookupObject("notificationLogger");

        // Need to explicitly dispose manager here to get disposal notifications
        muleContext.dispose();
        // allow shutdown to complete (or get concurrent mod errors and/or miss
        // notifications)

        PollingProber prober = new PollingProber(30000, 2000);
        prober.check(new JUnitProbe()
        {

            @Override
            protected boolean test() throws Exception
            {
                logNotifications();
                RestrictedNode spec = getSpecification();
                validateSpecification(spec);
                assertExpectedNotifications(spec);

                return true;
            }

            @Override
            public String describeFailure()
            {
                return "expected notifications not matched";
            }
        });
    }

    public abstract void doTest() throws Exception;

    public abstract RestrictedNode getSpecification();

    public abstract void validateSpecification(RestrictedNode spec) throws Exception;

    protected void logNotifications()
    {
        logger.info("Number of notifications: " + notificationLogger.getNotifications().size());
        for (Iterator<?> iterator = notificationLogger.getNotifications().iterator(); iterator.hasNext();)
        {
            ServerNotification notification = (ServerNotification) iterator.next();
            logger.info(notification);
        }
    }

    /**
     * This is destructive - do not use spec after calling this routine
     */
    protected void assertExpectedNotifications(RestrictedNode spec)
    {
        for (Iterator<?> iterator = notificationLogger.getNotifications().iterator(); iterator.hasNext();)
        {
            ServerNotification notification = (ServerNotification) iterator.next();
            switch (spec.match(notification))
            {
                case Node.SUCCESS :
                    break;
                case Node.FAILURE :
                    fail("Could not match " + notification);
                    break;
                case Node.EMPTY :
                    fail("Extra notification: " + notification);
            }
        }
        if (!spec.isExhausted())
        {
            fail("Specification not exhausted: " + spec.getAnyRemaining());
        }
    }

    protected void verifyAllNotifications(RestrictedNode spec, Class<?> clazz, int from, int to)
    {
        for (int action = from; action <= to; ++action)
        {
            if (!spec.contains(clazz, action))
            {
                fail("Specification missed action " + action + " for class " + clazz);
            }
        }
    }

    protected void verifyNotification(RestrictedNode spec, Class<?> clazz, int action)
    {
        if (!spec.contains(clazz, action))
        {
            fail("Specification missed action " + action + " for class " + clazz);
        }
    }
}
