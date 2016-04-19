/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.vm.functional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class VMRequestorTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "vm/vm-functional-test-flow.xml";
    }

    @Test
    public void testRequestorWithUpdateonMessage() throws Exception
    {
        for (int i = 0; i < 10; i++)
        {
            makeClientRequest("test" + i);
        }

        MuleClient client = muleContext.getClient();
        List<String> results = new ArrayList<String>();
        MuleMessage result = null;
        for (int i = 0; i < 10; i++)
        {
            result = client.request("vm://out", 3000L);
            assertNotNull(result);
            results.add(getPayloadAsString(result));
        }

        assertEquals(10, results.size());

        //This would fail if the owner thread info was not updated
        result.setOutboundProperty("foo", "bar");
    }

    protected void makeClientRequest(final String message) throws MuleException
    {
        final MuleClient client = muleContext.getClient();
        Thread t = new Thread(new Runnable()
        {
            @Override
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
