/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import org.junit.After;
import org.mule.api.context.notification.ServerNotification;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.Iterator;

import static org.junit.Assert.fail;

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

    @After
    public void clearNotifications()
    {
        if (notificationLogger != null)
        {
            notificationLogger.getNotifications().clear();
        }
    }

    protected void assertNotifications()
    {
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
                String notificationsLog = buildLogNotifications();
                RestrictedNode spec = getSpecification();
                validateSpecification(spec);
                assertExpectedNotifications(notificationsLog, spec);

                return true;
            }
        });
    }

    public abstract RestrictedNode getSpecification();

    public abstract void validateSpecification(RestrictedNode spec) throws Exception;

    protected String buildLogNotifications()
    {
        final StringBuilder logMessageBuilder = new StringBuilder();
        logMessageBuilder.append("Number of notifications: " + notificationLogger.getNotifications().size() + System.lineSeparator());
        for (Iterator<?> iterator = notificationLogger.getNotifications().iterator(); iterator.hasNext();)
        {
            ServerNotification notification = (ServerNotification) iterator.next();
            logMessageBuilder.append("\t" + notification + System.lineSeparator());
        }
        
        return logMessageBuilder.toString();
    }

    /**
     * This is destructive - do not use spec after calling this routine
     * @param notificationsLog 
     */
    protected void assertExpectedNotifications(String notificationsLog, RestrictedNode spec)
    {
        for (Iterator<?> iterator = notificationLogger.getNotifications().iterator(); iterator.hasNext();)
        {
            ServerNotification notification = (ServerNotification) iterator.next();
            switch (spec.match(notification))
            {
                case Node.SUCCESS :
                    break;
                case Node.FAILURE :
                    fail("Could not match " + notification + System.lineSeparator() + notificationsLog);
                    break;
                case Node.EMPTY :
                    fail("Extra notification: " + notification + System.lineSeparator() + notificationsLog);
            }
        }
        if (!spec.isExhausted())
        {
            fail("Specification not exhausted: " + spec.getAnyRemaining() + System.lineSeparator() + notificationsLog);
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
