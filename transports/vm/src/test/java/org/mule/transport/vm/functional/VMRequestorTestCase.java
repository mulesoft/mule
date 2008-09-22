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

public class VMRequestorTestCase extends FunctionalTestCase
{
    
    protected String getConfigResources()
    {
        return "vm/vm-functional-test.xml";
    }

    public void testRequestorWithUpdateonMessage() throws Exception
    {
        final MuleClient client = new MuleClient();
        Thread t = new Thread(new Runnable() 
        {
            public void run()
            {
                try
                {
                    client.send("vm://in", "test", null);
                }
                catch (MuleException e)
                {
                    fail("failed to dispatch event: " + e);
                    e.printStackTrace();
                }
            }
        }, "test-thread");
        t.start();
        
        MuleMessage result = client.request("vm://out", 3000L);
        assertNotNull(result);

        //This would fail if the owner thread info was not updated
        result.setProperty("foo", "bar");
    }
    
}
