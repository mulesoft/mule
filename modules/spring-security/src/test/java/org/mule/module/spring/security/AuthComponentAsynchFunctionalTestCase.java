/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.EncryptionStrategy;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.config.MuleProperties;
import org.mule.security.MuleCredentials;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;


public class AuthComponentAsynchFunctionalTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "auth-component-asynch-test.xml";
    }

    @Override
    public void doTearDown()
    {
        // Clear the security context after each test.
        SecurityContextHolder.setContext(new SecurityContextImpl());
    }

    @Test
    public void testCaseGoodAuthenticationGoodAuthorisation() throws Exception
    {
        MuleClient client = muleContext.getClient();

        EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("marie", "marie", "PBE", strategy);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        client.dispatch("vm://test", "Marie", props);
        MuleMessage m = client.request("vm://output", 3000);
        assertNotNull(m);
        assertEquals(m.getPayload(), "Marie");
    }

    @Test
    public void testCaseGoodAuthenticationBadAuthorisation() throws Exception
    {
        MuleClient client = muleContext.getClient();

        EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anon", "anon", "PBE", strategy);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        client.dispatch("vm://test", "Marie", props);
        MuleMessage m = client.request("vm://output", 3000);
        assertNull(m);
    }

    @Test
    public void testCaseBadAuthentication() throws Exception
    {
        MuleClient client = muleContext.getClient();

        EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");
        String header = MuleCredentials.createHeader("anonX", "anonX", "PBE", strategy);

        Map<String, Object> props = new HashMap<String, Object>();
        props.put(MuleProperties.MULE_USER_PROPERTY, header);

        client.dispatch("vm://test", "USD,MTL", props);
        MuleMessage m = client.request("vm://output", 3000);
        assertNull(m);
    }
}
