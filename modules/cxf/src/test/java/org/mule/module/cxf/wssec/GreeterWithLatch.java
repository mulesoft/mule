/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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


