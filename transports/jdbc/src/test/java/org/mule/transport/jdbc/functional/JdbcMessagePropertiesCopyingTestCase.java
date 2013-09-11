/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.NullPayload;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class JdbcMessagePropertiesCopyingTestCase extends AbstractJdbcFunctionalTestCase
{
    private static final String PROPERTY_KEY = "custom-key";
    private static final String PROPERTY_VALUE = "custom-value";

    public JdbcMessagePropertiesCopyingTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, AbstractJdbcFunctionalTestCase.getConfig() + ", jdbc-message-properties-copying-service.xml"},
            {ConfigVariant.FLOW, AbstractJdbcFunctionalTestCase.getConfig() + ", jdbc-message-properties-copying-flow.xml"}
        });
    }

    @Test
    public void testMessagePropertiesCopying() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        // provide a valid type header so the JDBC query actually returns something
        message.setOutboundProperty("type", 1);
        message.setOutboundProperty(PROPERTY_KEY, PROPERTY_VALUE);

        MuleMessage result = client.send("vm://in", message);
        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals(PROPERTY_VALUE, result.getInboundProperty(PROPERTY_KEY));
    }
}
