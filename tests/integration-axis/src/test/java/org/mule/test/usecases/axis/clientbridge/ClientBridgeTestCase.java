/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.axis.clientbridge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ClientBridgeTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/axis/clientbridge/client-mule-config.xml";
    }

    @Test
    public void testBridgeVMToAxis() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage message = client.send("vm://complexRequest", new ComplexData("Foo", new Integer(84)), null);
        assertNotNull(message);
        assertTrue(message.getPayload() instanceof ComplexData);

        ComplexData result = (ComplexData)message.getPayload();
        assertEquals(new Integer(84), result.getSomeInteger());
        assertEquals("Foo", result.getSomeString());
    }
}
