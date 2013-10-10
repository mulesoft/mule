/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport.jms;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ComponentBindingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/jms/nestedrouter-test.xml";
    }

    @Test
    public void testBinding() throws MuleException
    {
        MuleClient client = new MuleClient(muleContext);
        String message = "Mule";
        client.dispatch("jms://invoker.in", message, null);
        MuleMessage reply = client.request("jms://invoker.out", 10000);
        assertNotNull(reply);
        assertEquals("Received: Hello " + message + " " + 0xC0DE, reply.getPayload());
    }
}
