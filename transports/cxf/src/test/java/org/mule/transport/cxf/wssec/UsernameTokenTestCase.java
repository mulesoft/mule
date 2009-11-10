/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.wssec;
import org.mule.tck.FunctionalTestCase;

import org.apache.hello_world_soap_http.GreeterImpl;

public class UsernameTokenTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/transport/cxf/wssec/cxf-secure-service.xml, org/mule/transport/cxf/wssec/username-token-conf.xml";
    }
    
    @Override
    protected void doSetUp() throws Exception
    {
        ClientPasswordCallback.setPassword("secret");        
        super.doSetUp();
    }

    public void testUsernameToken() throws Exception
    {
        GreeterImpl impl = getGreeter();
        
        int i = 0;
        while (i < 100)
        {
            if (impl.getInvocationCount() > 0)
            {
                break;
            }
            Thread.sleep(50);
        }
        
        assertEquals(1, impl.getInvocationCount());
    }

    private GreeterImpl getGreeter() throws Exception
    {
        Object instance = getComponent("greeterService");
        
        return (GreeterImpl) instance;
    }
}


