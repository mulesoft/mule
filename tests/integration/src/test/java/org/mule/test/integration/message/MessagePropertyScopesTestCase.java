/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MessagePropertyScopesTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/messaging/message-property-scopes-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/messaging/message-property-scopes-config-flow.xml"}
        });
    }

    public MessagePropertyScopesTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testSessionProperty() throws Exception {

        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in1", "Hello World", null);
        assertNotNull(response);
        String payload = response.getPayloadAsString();
        assertNotNull(payload);
        assertEquals("java.util.Date", payload);
    }

    @Ignore
    @Test
    public void testInvocationProperty() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in2", "Hello World", null);
        // scope = "invocation" should not propagate the property on to the next service
        assertTrue(response.getPayload() instanceof NullPayload);
    }
}
