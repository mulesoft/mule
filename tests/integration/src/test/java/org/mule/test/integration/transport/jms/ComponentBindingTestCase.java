/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.transport.jms;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class ComponentBindingTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/integration/providers/jms/nestedrouter-test.xml";
    }

    public void testBinding() throws MuleException
    {
        MuleClient client = new MuleClient();
        String message = "Mule";
        client.dispatch("jms://invoker.in", message, null);
        MuleMessage reply = client.request("jms://invoker.out", 10000);
        assertNotNull(reply);
        assertEquals("Received: Hello " + message + " " + 0xC0DE, reply.getPayload());
    }
}
