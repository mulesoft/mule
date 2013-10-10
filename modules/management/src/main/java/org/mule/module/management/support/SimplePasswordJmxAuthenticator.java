/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.management.support;

import org.mule.module.management.agent.ConfigurableJMXAuthenticator;
import org.mule.module.management.agent.JmxAgent;
import org.mule.util.StringUtils;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXPrincipal;
import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A JMX authenticator for a simple username/password scheme.
 * Passwords are neither encrypted, nor obfuscated.
 */
public class SimplePasswordJmxAuthenticator implements JMXAuthenticator, ConfigurableJMXAuthenticator
{
    /**
     * Logger used by this class.
     */
    protected static final Log logger = LogFactory.getLog(JmxAgent.class);

    /**
     * An in-memory credentials storage.
     */
    private Map<String, Object> credentials = new HashMap<String, Object>();

    public Subject authenticate (Object authToken)
    {
        if (authToken == null)
        {
            throw new SecurityException("No authentication token available");
        }
        if (!(authToken instanceof String[]) || ((String[]) authToken).length != 2)
        {
            throw new SecurityException("Unsupported credentials format");
        }

        String[] authentication = (String[]) authToken;

        String username = StringUtils.defaultString(authentication[0]);
        String password = StringUtils.defaultString(authentication[1]);

        if (!credentials.containsKey(username))
        {
            throw new SecurityException("Unauthenticated user: " + username);
        }

        Object pass = credentials.get(username);
        if (!password.equals(pass == null ? "" : pass.toString()))
        {
            throw new SecurityException("Invalid password");
        }

        Set<Principal> principals = new HashSet<Principal>();
        principals.add(new JMXPrincipal(username));
        return new Subject(true, principals, Collections.EMPTY_SET, Collections.EMPTY_SET);
    }

    public void setCredentials (Map<String, String> newCredentials)
    {
        this.credentials.clear();
        if (newCredentials == null || newCredentials.isEmpty())
        {
            logger.warn("Credentials cache has been purged, remote access will no longer be available");
        }
        else
        {
            this.credentials.putAll(newCredentials);
        }
    }

    public void configure(Map newCredentials)
    {
        this.setCredentials(newCredentials);
    }
}
