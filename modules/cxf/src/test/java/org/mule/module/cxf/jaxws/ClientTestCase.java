/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.jaxws;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.DynamicPortTestCase;

import java.util.HashMap;
import java.util.Map;

import org.apache.hello_world_soap_http.GreeterImpl;

public class ClientTestCase extends DynamicPortTestCase
{
    public void testGeneratedClientWithQuartz() throws Exception
    {
        GreeterImpl impl = getGreeter();
        
        Thread.sleep(5000);
        
        assertEquals(1, impl.getInvocationCount());
    }

    private GreeterImpl getGreeter() throws Exception
    {
        Object instance = getComponent("greeterService");
        
        return (GreeterImpl) instance;
    }

    public void testClientWithMuleClient() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("operation", "greetMe");
        MuleMessage result = client.send("clientEndpoint", "Dan", props);
        assertEquals("Hello Dan", result.getPayload());
        
        GreeterImpl impl = getGreeter();
        
        Thread.sleep(5000);
        
        assertEquals(2, impl.getInvocationCount());
    }
    
    protected String getConfigResources()
    {
        return "jaxws-client-conf.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 1;
    }

}
