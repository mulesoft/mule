/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.listener.ExceptionListener;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class ExceptionNotificationTestCase extends AbstractNotificationTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/notifications/exception-notification-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/notifications/exception-notification-test-flow.xml"}
        });
    }

    public ExceptionNotificationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    public void doTest() throws Exception
    {
        ExceptionListener exceptionListener = new ExceptionListener(muleContext);
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("vm://in-1", new DefaultMuleMessage("hello world", muleContext));
        // processing is async, give time for the exception notificator to run
        exceptionListener.waitUntilAllNotificationsAreReceived();

        assertNull(result);
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node(ExceptionNotification.class, ExceptionNotification.EXCEPTION_ACTION);
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
        verifyAllNotifications(spec, ExceptionNotification.class,
                ExceptionNotification.EXCEPTION_ACTION, ExceptionNotification.EXCEPTION_ACTION);
    }
}
