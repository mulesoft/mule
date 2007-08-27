/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.support;

import org.mule.tck.AbstractMuleTestCase;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;

public class SimplePasswordJmxAuthenticatorTestCase extends AbstractMuleTestCase
{
    private static final String[] VALID_AUTH_TOKEN = {"mule", "mulepassword"};
    private SimplePasswordJmxAuthenticator authenticator;

    protected void doSetUp () throws Exception
    {
        super.doSetUp();
        authenticator = new SimplePasswordJmxAuthenticator();
    }

    public void testSuccessfulAuthentication()
    {
        Map credentials = getValidCredentials();

        authenticator.setCredentials(credentials);

        Subject subject = authenticator.authenticate(VALID_AUTH_TOKEN);
        assertNotNull(subject);
        assertTrue(subject.isReadOnly());

        final Set publicCredentials = subject.getPublicCredentials();
        assertNotNull(publicCredentials);
        assertEquals(0, publicCredentials.size());

        final Set privateCredentials = subject.getPrivateCredentials();
        assertNotNull(privateCredentials);
        assertEquals(0, privateCredentials.size());

        final Set principals = subject.getPrincipals();
        assertNotNull(principals);
        assertEquals(1, principals.size());

        final Object ref = principals.iterator().next();
        assertTrue(ref instanceof JMXPrincipal);

        final JMXPrincipal jmxPrincipal = (JMXPrincipal) ref;
        final String name = jmxPrincipal.getName();
        assertNotNull(name);
        assertEquals(VALID_AUTH_TOKEN[0], name);
    }

    public void testNullOrEmptyCredentialsConfigured()
    {
        final Map credentials = Collections.EMPTY_MAP;

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

    protected Map getValidCredentials ()
    {
        final Map credentials = new HashMap(1);
        credentials.put(VALID_AUTH_TOKEN[0], VALID_AUTH_TOKEN[1]);

        return credentials;
    }

}