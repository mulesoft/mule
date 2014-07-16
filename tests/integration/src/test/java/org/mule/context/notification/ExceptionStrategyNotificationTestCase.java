/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.context.notification;

import static org.junit.Assert.assertNotNull;
import static org.mule.context.notification.ExceptionStrategyNotification.PROCESS_END;
import static org.mule.context.notification.ExceptionStrategyNotification.PROCESS_START;

import org.mule.api.client.MuleClient;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized;

public class ExceptionStrategyNotificationTestCase extends AbstractNotificationTestCase
{
    public ExceptionStrategyNotificationTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {{ConfigVariant.FLOW,
                "org/mule/test/integration/notifications/exception-strategy-notification-test-flow.xml"}});
    }

    @Override
    public void doTest() throws Exception
    {
        MuleClient client = muleContext.getClient();
        assertNotNull(client.send("vm://in-catch", "test", null));
        assertNotNull(client.send("vm://in-rollback", "test", null));
        assertNotNull(client.send("vm://in-choice-es", "test", null));
        assertNotNull(client.send("vm://in-default-es", "test", null));
    }

    @Override
    public RestrictedNode getSpecification()
    {
        return new Node()
                .serial(node(PROCESS_START).serial(node(PROCESS_END)))
                .serial(node(PROCESS_START).serial(node(PROCESS_END)))
                .serial(node(PROCESS_START).serial(node(PROCESS_END)))
                .serial(node(PROCESS_START).serial(node(PROCESS_END)))
                ;
    }

    private RestrictedNode node(int action)
    {
        return new Node(ExceptionStrategyNotification.class, action);
    }

    @Override
    public void validateSpecification(RestrictedNode spec) throws Exception
    {
    }
}
