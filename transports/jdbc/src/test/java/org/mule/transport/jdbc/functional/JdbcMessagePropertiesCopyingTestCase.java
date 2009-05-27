/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jdbc.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.transport.NullPayload;

public class JdbcMessagePropertiesCopyingTestCase extends AbstractJdbcFunctionalTestCase
{    
    private static final String PROPERTY_KEY = "custom-key";
    private static final String PROPERTY_VALUE = "custom-value";

    protected String getConfigResources()
    {
        return super.getConfigResources() + ", jdbc-message-properties-copying.xml";
    }

    public void testMessagePropertiesCopying() throws Exception
    {
        MuleClient client = new MuleClient();
        
        MuleMessage message = new DefaultMuleMessage(TEST_MESSAGE);
        // provide a valid type header so the JDBC query actually returns something
        message.setProperty("type", Integer.valueOf(1));
        message.setProperty(PROPERTY_KEY, PROPERTY_VALUE);
        
        MuleMessage result = client.send("vm://in", message);
        assertNotNull(result);
        assertNull(result.getExceptionPayload());
        assertFalse(result.getPayload() instanceof NullPayload);
        assertEquals(PROPERTY_VALUE, result.getProperty(PROPERTY_KEY));
    }
}
