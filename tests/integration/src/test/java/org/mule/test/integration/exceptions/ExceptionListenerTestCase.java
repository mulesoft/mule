/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.message.ExceptionMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ExceptionListenerTestCase extends AbstractServiceAndFlowTestCase
{
    private MuleClient client;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
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
    }

    private void assertQueueIsEmpty(String queueName) throws MuleException
    {
        MuleMessage message = client.request(queueName, 2000);
        assertNull(message);
    }
}
