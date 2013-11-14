/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

public class ExceptionStrategyReturnMessageTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-return-message.xml";
    }

    @Test
    public void testReturnPayloadDefaultStrategy() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage msg = client.send("vm://in-default-strategy", "Test Message", null);

        assertNotNull(msg);
        assertNotNull(msg.getExceptionPayload());
        assertEquals("Functional Test Service Exception", msg.getExceptionPayload().getMessage());

        assertNotNull(msg.getPayload());
        assertTrue(msg.getPayload() instanceof NullPayload);
    }

    @Test
    public void testReturnPayloadCustomStrategy() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage msg = client.send("vm://in-custom-strategy", "Test Message", null);

        assertNotNull(msg);
        assertNotNull(msg.getExceptionPayload());
        assertEquals("Functional Test Service Exception", msg.getExceptionPayload().getMessage());

        assertNotNull(msg.getPayload());
        assertEquals("Ka-boom!", msg.getPayload());
    }
}
