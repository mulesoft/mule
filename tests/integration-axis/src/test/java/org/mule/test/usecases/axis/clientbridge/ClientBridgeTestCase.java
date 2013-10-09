/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.axis.clientbridge;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ClientBridgeTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/axis/clientbridge/client-mule-config.xml";
    }

    @Test
    public void testBridgeVMToAxis() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("vm://complexRequest", new ComplexData("Foo", new Integer(84)), null);

        assertNotNull(message);
        assertTrue(message.getPayload() instanceof ComplexData);
        ComplexData result = (ComplexData)message.getPayload();
        assertEquals(new Integer(84), result.getSomeInteger());
        assertEquals("Foo", result.getSomeString());
    }

}
