/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.NullPayload;

import org.junit.Test;

public class ExceptionStrategyReturnMessageTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-return-message.xml";
    }

    @Test
    public void testReturnPayloadDefaultStrategy() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage msg = client.send("vm://in-custom-strategy", "Test Message", null);

        assertNotNull(msg);
        assertNotNull(msg.getExceptionPayload());
        assertEquals("Functional Test Service Exception", msg.getExceptionPayload().getMessage());

        assertNotNull(msg.getPayload());
        assertEquals("Ka-boom!", msg.getPayload());
    }
}
