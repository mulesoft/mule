/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.soap.axis;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.http.HttpsConnector;

public class AxisConnectorHttpsTestCase extends FunctionalTestCase
{
    
    public void testHttpsConnection() throws Exception{
        MuleClient client = new MuleClient();
        MuleMessage m = client.send("axis:https://localhost:62000/TestUMO?method=echo",new DefaultMuleMessage("hello"));
        assertNotNull(m);
        
        // check that our https connector is being used
        MuleEvent event = RequestContext.getEvent();
        assertTrue (event.getEndpoint().getConnector() instanceof HttpsConnector);
        assertTrue(event.getEndpoint().getConnector().getName().equals("myHttpsConnector"));
    }
    
    protected String getConfigResources()
    {
        return "axis-https-connector-config.xml";
    }
    
}


