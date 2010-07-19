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
import org.mule.util.concurrent.Latch;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class UsernameTokenTestCase extends FunctionalTestCase
{
    private Latch greetLatch;
    
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
        
        greetLatch = getGreeter().getLatch();
    }

    public void testUsernameToken() throws Exception
    {
        assertTrue(greetLatch.await(60, TimeUnit.SECONDS));
    }

    private GreeterWithLatch getGreeter() throws Exception
    {
        Object instance = getComponent("greeterService");
        return (GreeterWithLatch) instance;
    }
}


