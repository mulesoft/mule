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
import static org.mule.api.transport.PropertyScope.SESSION;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.transport.NullPayload;

import org.junit.Test;

public class JdbcSessionVariablesCopyingTestCase extends AbstractJdbcFunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "jdbc-session-variables-copying-flow.xml";
    }

    @Test
    public void testMessagePropertiesCopying() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE, muleContext);
        // provide a valid type header so the JDBC query actually returns something
        message.setOutboundProperty("type", 1);

        MuleMessage result = client.send("vm://in", message);
        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals("test", result.getProperty("test", SESSION));
    }
}
