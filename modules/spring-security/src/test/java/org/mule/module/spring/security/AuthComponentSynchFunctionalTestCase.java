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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleMessage;
import org.mule.api.config.MuleProperties;
import org.mule.api.security.UnauthorisedException;
import org.mule.component.ComponentException;
import org.mule.module.client.MuleClient;
import org.mule.security.MuleCredentials;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

public class AuthComponentSynchFunctionalTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "auth-component-synch-test.xml";
    }

    @Override
    // Clear the security context after each test.
    public void doTearDown()
    {
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }

    @Test
    public void testCaseGoodAuthenticationGoodAuthorisation() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();

        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("marie", "marie", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        MuleMessage m = client.send("vm://test", "Marie", props);
        assertNotNull(m);
        assertTrue(m.getPayload().equals("Marie"));
    }

    @Test
    public void testCaseGoodAuthenticationBadAuthorisation() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();

        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anon", "anon", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        MuleMessage result = client.send("vm://test", "Marie", props);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(ComponentException.class, result.getExceptionPayload().getException().getClass());
    }

    @Test
    public void testCaseBadAuthentication() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        Map props = new HashMap();

        EncryptionStrategy strategy = muleContext
            .getSecurityManager()
            .getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anonX", "anonX", "PBE", strategy);
        props.put(MuleProperties.MULE_USER_PROPERTY, header);
        MuleMessage result = client.send("vm://test", "Marie", props);
        assertNotNull(result);
        assertNotNull(result.getExceptionPayload());
        assertEquals(UnauthorisedException.class, result.getExceptionPayload().getException().getClass());
    }

}
