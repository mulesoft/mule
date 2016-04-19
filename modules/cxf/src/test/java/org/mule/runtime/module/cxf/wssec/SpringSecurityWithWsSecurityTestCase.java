/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.wssec;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.core.api.security.SecurityContext;

import org.junit.Ignore;

@Ignore("MULE-6926: flaky test")
public class SpringSecurityWithWsSecurityTestCase extends UsernameTokenTestCase
{

    @Override
    protected String[] getConfigFiles()
    {
        return new String[] {
                             "org/mule/runtime/module/cxf/wssec/cxf-secure-service-security-manager-ws-security-flow-httpn.xml",
                             "org/mule/runtime/module/cxf/wssec/spring-security-ws-security-conf.xml"
        };
    }

    @Override
    @Ignore("MULE-6926: flaky test")
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
