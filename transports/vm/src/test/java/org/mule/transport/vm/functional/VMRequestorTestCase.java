/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.functional;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;

public class VMRequestorTestCase extends FunctionalTestCase
{
    
    protected String getConfigResources()
    {
        return "vm/vm-functional-test.xml";
    }

    public void testRequestorWithUpdateonMessage() throws Exception
    {

        for (int i = 0; i < 10; i++)
        {
            makeClientRequest("test" + i);
        }

        MuleClient client = new MuleClient();
        List results = new ArrayList();
        MuleMessage result = null;
        for (int i = 0; i < 10; i++)
        {
            result = client.request("vm://out", 3000L);
            assertNotNull(result);
            results.add(result.getPayloadAsString());
        }

        assertEquals(10, results.size());

        //This would fail if the owner thread info was not updated
        result.setProperty("foo", "bar");
    }

    protected void makeClientRequest(final String message) throws MuleException
    {
        final MuleClient client = new MuleClient();
        Thread t = new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    client.send("vm://in", message, null);
                }
                catch (MuleException e)
                {
                    fail("failed to dispatch event: " + e);
                    e.printStackTrace();
                }
            }
        }, "test-thread");
        t.start();
    }
    
}
