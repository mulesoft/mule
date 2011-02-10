/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.wsrm;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.DynamicPortTestCase;

public class WSRMTest extends DynamicPortTestCase
{
    public void testAnonymous() throws Exception
    {
        MuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage result = client.send("anonymousReplyClientEndpoint", new DefaultMuleMessage("test", muleContext));        
        assertEquals("Hello test", result.getPayloadAsString());
    }

    public void testDecoupled() throws Exception
    {
        MuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage result = client.send("decoupledClientEndpoint", new DefaultMuleMessage("test", muleContext));        
        assertEquals("Hello test", result.getPayloadAsString());
    }
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/cxf/wsrm/wsrm-conf.xml";
    }

    @Override
    protected int getNumPortsToFind()
    {
        return 2;
    }

}


