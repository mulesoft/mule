/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.cxf.wssec;

import static org.junit.Assert.*;

import org.mule.api.security.SecurityContext;

import java.util.Arrays;
import java.util.Collection;

import org.junit.runners.Parameterized.Parameters;

public class SpringSecurityWithWsSecurityTestCase extends UsernameTokenTestCase
{
    public SpringSecurityWithWsSecurityTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/cxf/wssec/cxf-secure-service-security-manager-ws-security-service.xml, org/mule/module/cxf/wssec/spring-security-ws-security-conf.xml"},
            {ConfigVariant.FLOW, "org/mule/module/cxf/wssec/cxf-secure-service-security-manager-ws-security-flow.xml, org/mule/module/cxf/wssec/spring-security-ws-security-conf.xml"}
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
        assertEquals(null, sc.getAuthentication().getCredentials());
        assertNotNull(sc.getAuthentication().getPrincipal());
    }
}
