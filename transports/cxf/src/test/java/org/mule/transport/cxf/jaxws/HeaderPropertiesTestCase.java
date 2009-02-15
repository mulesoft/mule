/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.jaxws;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.testmodels.mule.TestSedaService;

import java.util.HashMap;
import java.util.Map;

import org.apache.hello_world_soap_http.GreeterImpl;

public class HeaderPropertiesTestCase extends FunctionalTestCase
{

    private GreeterImpl getGreeter() throws Exception
    {
        Object instance = getComponent("greeterService");
        
        return (GreeterImpl) instance;
    }

    public void testClientWithMuleClient() throws Exception
    {
        final TestSedaService testSedaService = (TestSedaService) muleContext.getRegistry().lookupService("testService");
        FunctionalTestComponent testComponent = (FunctionalTestComponent) getComponent(testSedaService);
        assertNotNull(testComponent);

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(final MuleEventContext context, final Object component) throws Exception
            {
                MuleMessage msg = context.getMessage();
                assertEquals("BAR", msg.getProperty("FOO"));
                assertNull(msg.getProperty("clientClass"));
            }
        };
        testComponent.setEventCallback(callback);
        
        MuleClient client = new MuleClient();
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("operation", "greetMe");
        props.put("FOO", "BAR");
        MuleMessage result = client.send("clientEndpoint", "Dan", props);
        assertEquals("Hello Dan Received", result.getPayload());
        
        GreeterImpl impl = getGreeter();
        assertEquals(1, impl.getInvocationCount());
    }
    
    protected String getConfigResources()
    {
        return "header-conf.xml";
    }

}