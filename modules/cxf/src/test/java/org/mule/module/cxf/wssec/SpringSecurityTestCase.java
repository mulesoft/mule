/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.wssec;

import static org.junit.Assert.*;

import org.mule.api.security.SecurityContext;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SpringSecurityTestCase extends UsernameTokenTestCase
{
    public SpringSecurityTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/cxf/wssec/cxf-secure-service-service.xml, org/mule/module/cxf/wssec/spring-security-conf.xml"},
            {ConfigVariant.FLOW, "org/mule/module/cxf/wssec/cxf-secure-service-flow.xml, org/mule/module/cxf/wssec/spring-security-conf.xml"}
        });
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
