/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.message;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.NonSerializableObject;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * see EE-2307
 *
 * Non serializable session properties won't be serialized with the mule session
 * but neither it will be lost in the flow were it was stored
 */
public class SessionPropertyChainingRouterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/messaging/session-property-chaining-router.xml";
    }

    @Test
    public void testRouter() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://in", "test message", null);
        assertNotNull(response);
        assertTrue("Response is " + response.getPayload(), response.getPayload() instanceof NonSerializableObject);
    }
}


