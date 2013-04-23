/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.wssec;

import org.mule.api.security.SecurityContext;

import static org.junit.Assert.*;

public class SpringSecurityTestCase extends UsernameTokenTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/cxf/wssec/cxf-secure-service.xml, org/mule/module/cxf/wssec/spring-security-conf.xml";
    }
    
    @Override
    public void testUsernameToken() throws Exception
    {
        super.testUsernameToken();
        GreeterWithLatch greeter = getGreeter();
        SecurityContext sc = greeter.getSecurityContext();
        assertNotNull(sc);
        assertNotNull(sc.getAuthentication());
        assertEquals("secret", sc.getAuthentication().getCredentials());
        assertNotNull(sc.getAuthentication().getPrincipal());
    }
}
