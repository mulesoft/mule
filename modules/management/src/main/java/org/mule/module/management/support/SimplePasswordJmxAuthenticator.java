/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.management.support;

import org.mule.module.management.agent.ConfigurableJMXAuthenticator;
import org.mule.module.management.agent.JmxApplicationAgent;
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
    protected static final Log logger = LogFactory.getLog(JmxApplicationAgent.class);

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
