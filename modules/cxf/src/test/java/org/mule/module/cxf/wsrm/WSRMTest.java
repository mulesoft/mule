/*
 * $Id: WSATest.java 20320 2010-11-24 15:03:31Z dfeist $
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
import org.mule.tck.FunctionalTestCase;

public class WSRMTest extends FunctionalTestCase
{
    public void testWSRM() throws Exception
    {
        MuleClient client = new DefaultLocalMuleClient(muleContext);
        MuleMessage result = client.send("clientEndpoint", new DefaultMuleMessage("test", muleContext));        
        System.out.println(result);
    }
    
    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/cxf/wsrm/wsrm-conf.xml";
    }

}


