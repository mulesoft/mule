/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.context.notification.ExceptionStrategyNotificationListener;
import org.mule.api.context.notification.ServerNotification;
import org.mule.context.notification.ExceptionStrategyNotification;
import org.mule.message.ExceptionMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ExceptionListenerTestCase extends AbstractServiceAndFlowTestCase
{

    private static final int TIMEOUT_MILLIS = 5000;
    private static final int POLL_DELAY_MILLIS = 100;

    private MuleClient client;
    private ServerNotification exceptionStrategyStartNotification;
    private ServerNotification exceptionStrategyEndNotification;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/exception-listener-config-service.xml"},
                {ConfigVariant.FLOW, "org/mule/test/integration/exceptions/exception-listener-config-flow.xml"}
        });
    }

    public ExceptionListenerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        client = muleContext.getClient();

        exceptionStrategyStartNotification = null;
        exceptionStrategyEndNotification = null;
        muleContext.getNotificationManager().addListener(new ExceptionStrategyNotificationListener<ExceptionStrategyNotification>()
        {
            @Override
            public void onNotification(ExceptionStrategyNotification notification)
            {
                if (notification.getAction() == ExceptionStrategyNotification.PROCESS_START)
                {
                    exceptionStrategyStartNotification = notification;
                }
                else if (notification.getAction() == ExceptionStrategyNotification.PROCESS_END)
                {
                    exceptionStrategyEndNotification = notification;
                }
            }
        });
    }

    @Test
    public void testExceptionStrategyFromComponent() throws Exception
    {
        assertQueueIsEmpty("vm://error.queue");

        client.send("vm://component.in", "test", null);

        assertQueueIsEmpty("vm://component.out");

        MuleMessage message = client.request("vm://error.queue", 2000);
        assertNotNull(message);
        Object payload = message.getPayload();
        assertTrue(payload instanceof ExceptionMessage);

        assertNotificationsArrived();
        assertNotificationsHaveMatchingResourceIds();
    }

    private void assertNotificationsHaveMatchingResourceIds()
    {
        assertThat(exceptionStrategyStartNotification.getResourceIdentifier(), is(not(nullValue())));
        assertThat(exceptionStrategyStartNotification.getResourceIdentifier(), is("mycomponent"));
        assertThat(exceptionStrategyStartNotification.getResourceIdentifier(), is(exceptionStrategyEndNotification.getResourceIdentifier()));
    }

    private void assertNotificationsArrived()
    {
        PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
        prober.check(new JUnitProbe()
        {
            @Override
            protected boolean test() throws Exception
            {
                assertThat(exceptionStrategyStartNotification, is(not(nullValue())));
                assertThat(exceptionStrategyEndNotification, is(not(nullValue())));
                return true;
            }

            @Override
            public String describeFailure()
            {
                return "Did not get exception strategy notifications";
            }
        });
    }

    private void assertQueueIsEmpty(String queueName) throws MuleException
    {
        MuleMessage message = client.request(queueName, 2000);
        assertNull(message);
    }
}
