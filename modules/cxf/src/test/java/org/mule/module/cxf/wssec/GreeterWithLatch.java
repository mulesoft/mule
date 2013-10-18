/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

import org.mule.RequestContext;
import org.mule.api.security.SecurityContext;
import org.mule.util.concurrent.Latch;

import org.apache.hello_world_soap_http.GreeterImpl;

public class GreeterWithLatch extends GreeterImpl
{
    private Latch greetLatch = new Latch();
    private SecurityContext securityContext;

    @Override
    public String greetMe(String me)
    {
        String result = super.greetMe(me);
        greetLatch.countDown();
        securityContext = RequestContext.getEvent().getSession().getSecurityContext();
        return result;
    }
    
    public Latch getLatch()
    {
        return greetLatch;
    }
    
    public SecurityContext getSecurityContext()
    {
        return securityContext;
    }
}


