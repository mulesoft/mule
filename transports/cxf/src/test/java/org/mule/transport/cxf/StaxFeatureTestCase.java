/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import org.mule.tck.FunctionalTestCase;

import org.apache.cxf.Bus;

public class StaxFeatureTestCase extends FunctionalTestCase
{
    public void testEchoService() throws Exception
    {
        CxfConnector cxfCon = (CxfConnector)muleContext.getRegistry().lookupConnector("connector.cxf.0");
        assertNotNull(cxfCon);

        Bus cxfBus = cxfCon.getCxfBus();
        
        // ensure that our interceptor is there
//        assertEquals(2, cxfBus.getInInterceptors().size());
//        
//        MuleClient client = new MuleClient();
//        MuleMessage result = client.send("cxf:http://localhost:63081/services/Echo?method=echo", "Hello!", null);
//        assertEquals("Hello!", result.getPayload());
    }

    @Override
    protected String getConfigResources() 
    {
        return "stax-feature-conf.xml";
    }
}

