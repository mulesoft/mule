/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.spring.security;

import org.mule.api.MuleMessage;
import org.mule.client.DefaultLocalMuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.HashMap;

public class AuthenticateVmTransportTest extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "auth-vm-transport-config.xml";
    }

    public void testExplicitAttributes() throws Exception
    {
        testVM("vm://test");
    }

    public void testDefaultAttributes() throws Exception
    {
        testVM("vm://default-attributes");
    }
    
    public void testVM(String endpoint) throws Exception
    {
        DefaultLocalMuleClient client = new DefaultLocalMuleClient(muleContext);
        
        HashMap<String, Object> props = new HashMap<String,Object>();
        props.put("username", "ross");
        props.put("password", "ross");
        MuleMessage result = client.send(endpoint, "hi", props);
        assertNull(result.getExceptionPayload());
        
        props.put("password", "badpass");
        try
        {
            client.send(endpoint, "hi", props);
            fail("Exception expected");
        }
        catch (Exception e)
        {
            // expected
        }        
    }
}
