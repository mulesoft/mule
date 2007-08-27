/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.axis.clientbridge;

import org.mule.extras.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class ClientBridgeTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/usecases/axis/clientbridge/client-mule-config.xml";
    }

    public void testBridgeVMToAxis() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage message = client.send("vm://complexRequest", new ComplexData("Foo", new Integer(84)), null);

        assertNotNull(message);
        assertTrue(message.getPayload() instanceof ComplexData);
        ComplexData result = (ComplexData)message.getPayload();
        assertEquals(new Integer(84), result.getSomeInteger());
        assertEquals("Foo", result.getSomeString());

    }
}
