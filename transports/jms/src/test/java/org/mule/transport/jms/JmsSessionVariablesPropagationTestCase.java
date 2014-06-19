/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 *
 */
public class JmsSessionVariablesPropagationTestCase extends FunctionalTestCase
{
    protected String getConfigFile()
    {
        return "jms-session-propagation-config.xml";
    }

    @Test
    public void testSessionVariablePropagation() throws Exception
    {
        final String payload =  "some data";
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("jms://test", payload, null);
        assertNotNull(message);
        assertNull(message.getExceptionPayload());
        assertEquals(payload, message.getPayload());

        assertEquals("test", message.getSessionProperty("test"));
    }
}
