/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.issues;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class MultipleWsdlCxfCallsTestCase extends FunctionalTestCase
{
    public MultipleWsdlCxfCallsTestCase()
    {
        super();
        
        // Do not fail test case upon timeout because this probably just means
        // that the 3rd-party web service is off-line.
        setFailOnTimeout(false);
    }
    
    protected String getConfigResources()
    {
        return "multiple-wsdl-cxf-calls-config.xml";
    }

    public void testMultipleAsynchronousCalls() throws Exception
    {
        final MuleClient client = new MuleClient();

        for (int i = 0; i < 5; i++)
        {
            new Thread(new Runnable() 
            {
                public void run()
                {
                    try
                    {
                        client.dispatch("vm://receive", 
                            new DefaultMuleMessage(new Object[] {"USD", "EUR"}));
                    }
                    catch (MuleException e)
                    {
                        logger.error("Exception in test", e);
                    }
                }               
            }).start();
        }

        for (int i = 0; i < 5; i++)
        {
            MuleMessage result = client.request("vm://output", 20000L);
            assertNotNull(result);
            assertNotNull(result.getPayload());
        }
    }

    public void testMultipleSynchronousCalls() throws Exception
    {
        final MuleClient client = new MuleClient();

        for (int i = 0; i < 5; i++)
        {
            MuleMessage result = client.send("vm://receive", 
                new DefaultMuleMessage(new Object[] {"USD","EUR"}));
            assertNotNull(result.getPayload());    
        }
    }
}


