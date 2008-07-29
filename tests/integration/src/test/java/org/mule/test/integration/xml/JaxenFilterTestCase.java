/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.xml;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.io.InputStream;

import org.w3c.dom.Document;

public class JaxenFilterTestCase extends FunctionalTestCase
{
    public void testJaxen() throws Exception
    {
        MuleClient client = new MuleClient();
        InputStream po = getClass().getResourceAsStream("/org/mule/test/integration/xml/purchase-order.xml");
        
        assertNotNull(po);
        
        MuleMessage msg = new DefaultMuleMessage(po);
        MuleMessage res = client.send("vm://in", msg);
        
        Object payload = res.getPayload();
        assertTrue("payload is of type " + payload.getClass(), payload instanceof Document);
    }
    
    protected String getConfigResources()
    {
        return "org/mule/test/integration/xml/jaxen-routing-conf.xml";
    }

}


