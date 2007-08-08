/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.soap.axis;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;
import org.mule.providers.http.HttpsConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;

public class AxisConnectorHttpsTestCase extends FunctionalTestCase
{
    
    public void testHttpsConnection() throws Exception{
        MuleClient client = new MuleClient();
        UMOMessage m = client.send("axis:https://localhost:62000/TestUMO?method=echo",new MuleMessage("hello"));
        assertNotNull(m);
        
        // check that our https connector is being used
        UMOEvent event = RequestContext.getEvent();
        assertTrue (event.getEndpoint().getConnector() instanceof HttpsConnector);
        assertTrue(event.getEndpoint().getConnector().getName().equals("myHttpsConnector"));
    }
    
    protected String getConfigResources()
    {
        return "axis-https-connector-config.xml";
    }
    
}


