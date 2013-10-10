/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jdbc.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
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
        MuleClient client = new MuleClient(muleContext);
        
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
