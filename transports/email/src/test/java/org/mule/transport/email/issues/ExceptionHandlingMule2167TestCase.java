/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.email.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExceptionHandlingMule2167TestCase extends FunctionalTestCase
{

    public static final String MESSAGE = "a message";
    public static final long WAIT_MS = 3000L;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "exception-handling-mule-2167-test.xml";
    }

    @Test
    public void testDefaultConfig() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://in-default", MESSAGE, null);
        MuleMessage message = client.request("vm://out-default", WAIT_MS);
        assertNotNull("null message", message);
        assertNotNull("null payload", message.getPayload());
        assertEquals(MESSAGE, message.getPayloadAsString());
    }

}
