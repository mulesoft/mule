/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
