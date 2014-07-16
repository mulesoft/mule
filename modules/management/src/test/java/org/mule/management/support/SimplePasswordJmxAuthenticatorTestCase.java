/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.support;

import org.mule.module.management.support.SimplePasswordJmxAuthenticator;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SmallTest
public class SimplePasswordJmxAuthenticatorTestCase extends AbstractMuleTestCase
{
    private static final String[] VALID_AUTH_TOKEN = {"mule", "mulepassword"};
    private SimplePasswordJmxAuthenticator authenticator;

    @Before
    public void setUpAuthenticator () throws Exception
    {
        authenticator = new SimplePasswordJmxAuthenticator();
    }

    @Test
    public void testSuccessfulAuthentication()
    {
        Map<String, String> credentials = getValidCredentials();

        authenticator.setCredentials(credentials);

        Subject subject = authenticator.authenticate(VALID_AUTH_TOKEN);
        assertNotNull(subject);
        assertTrue(subject.isReadOnly());

        Set<Object> publicCredentials = subject.getPublicCredentials();
        assertNotNull(publicCredentials);
        assertEquals(0, publicCredentials.size());

        Set<Object> privateCredentials = subject.getPrivateCredentials();
        assertNotNull(privateCredentials);
        assertEquals(0, privateCredentials.size());

        Set<Principal> principals = subject.getPrincipals();
        assertNotNull(principals);
        assertEquals(1, principals.size());

        Object ref = principals.iterator().next();
        assertTrue(ref instanceof JMXPrincipal);

        JMXPrincipal jmxPrincipal = (JMXPrincipal) ref;
        String name = jmxPrincipal.getName();
        assertNotNull(name);
        assertEquals(VALID_AUTH_TOKEN[0], name);
    }

    @Test
    public void testNullOrEmptyCredentialsConfigured()
    {
        Map<String, String> credentials = Collections.emptyMap();

        // shouldn't fail
        authenticator.setCredentials(credentials);

        try
        {
            authenticator.authenticate(VALID_AUTH_TOKEN);
            fail("Should've thrown an exception");
        }
        catch (SecurityException e)
        {
            // expected
        }

        // shouldn't fail
        authenticator.setCredentials(null);

        try
        {
            authenticator.authenticate(VALID_AUTH_TOKEN);
            fail("Should've thrown an exception");
        }
        catch (SecurityException e)
        {
            // expected
        }

    }

    @Test
    public void testNullAuthToken()
    {
        try
        {
            authenticator.authenticate(null);
            fail("Should've thrown an exception");
        }
        catch (SecurityException e)
        {
            // expected
        }
    }

    @Test
    public void testInvalidAuthToken ()
    {
        try
        {
            final String token = "not a String array";
            authenticator.authenticate(token);
            fail("Should've thrown an exception");
        }
        catch (SecurityException e)
        {
            // expected
        }
    }

    @Test
    public void testAuthTokenTooLong()
    {
        try
        {
            final String[] token = {"token", "too", "long"};
            authenticator.authenticate(token);
            fail("Should've thrown an exception");
        }
        catch (SecurityException e)
        {
            // expected
        }
    }

    @Test
    public void testAuthTokenTooShort()
    {
        try
        {
            final String[] token = {"token_too_short"};
            authenticator.authenticate(token);
            fail("Should've thrown an exception");
        }
        catch (SecurityException e)
        {
            // expected
        }
    }

    @Test
    public void testNoSuchUser()
    {
        try
        {
            final String[] token = {"nosuchuser", "thepassword"};
            authenticator.authenticate(token);
            fail("Should've thrown an exception");
        }
        catch (SecurityException e)
        {
            // expected
        }
    }

    @Test
    public void testInvalidPassword()
    {
        authenticator.setCredentials(getValidCredentials());
        
        try
        {
            final String[] token = {"mule", "wrongpassword"};
            authenticator.authenticate(token);
            fail("Should've thrown an exception");
        }
        catch (SecurityException e)
        {
            // expected
        }
    }

    protected Map<String, String> getValidCredentials ()
    {
        Map<String, String> credentials = new HashMap<String, String>(1);
        credentials.put(VALID_AUTH_TOKEN[0], VALID_AUTH_TOKEN[1]);

        return credentials;
    }

}
