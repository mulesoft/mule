/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.functional.junit4.FunctionalTestCase;

import org.junit.Test;

public class ComponentBindingTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/providers/jms/nestedrouter-test.xml";
    }

    @Test
    public void testBinding() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        String message = "Mule";
        client.dispatch("jms://invoker.in", message, null);
        MuleMessage reply = client.request("jms://invoker.out", 10000);
        assertNotNull(reply);
        assertEquals("Received: Hello " + message + " " + 0xC0DE, reply.getPayload());
    }
}
