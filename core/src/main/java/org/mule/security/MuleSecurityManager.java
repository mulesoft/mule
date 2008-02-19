/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.security;

import org.mule.api.EncryptionStrategy;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.lifecycle.LifecycleLogic;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityManager;
import org.mule.api.security.SecurityProvider;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnknownAuthenticationTypeException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <code>MuleSecurityManager</code> is a default implementation security manager
 * for a Mule instance.
 */

public class MuleSecurityManager implements SecurityManager
{

    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(MuleSecurityManager.class);

    private Map providers = new ConcurrentHashMap();
    private Map cryptoStrategies = new ConcurrentHashMap();

    public MuleSecurityManager()
    {
        // for debug
    }

    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        List all = new LinkedList(providers.values());
        // ordering: appends
        all.addAll(cryptoStrategies.values());
        return LifecycleLogic.initialiseAll(all.iterator());
    }

    public Authentication authenticate(Authentication authentication)
        throws SecurityException, SecurityProviderNotFoundException
    {
        Iterator iter = providers.values().iterator();

        Class toTest = authentication.getClass();

        while (iter.hasNext())
        {
            SecurityProvider provider = (SecurityProvider) iter.next();

            if (provider.supports(toTest))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Authentication attempt using " + provider.getClass().getName());
                }

                Authentication result = provider.authenticate(authentication);

                if (result != null)
                {
                    return result;
                }
            }
        }

        throw new SecurityProviderNotFoundException(toTest.getName());
    }

    public void addProvider(SecurityProvider provider)
    {
        if (getProvider(provider.getName()) != null)
        {
            throw new IllegalArgumentException("Provider already registered: " + provider.getName());
        }
        providers.put(provider.getName(), provider);
    }

    public SecurityProvider getProvider(String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("provider Name cannot be null");
        }
        return (SecurityProvider) providers.get(name);
    }

    public SecurityProvider removeProvider(String name)
    {
        return (SecurityProvider) providers.remove(name);
    }

    public Collection getProviders()
    {
        return Collections.unmodifiableCollection(new ArrayList(providers.values()));
    }

    public void setProviders(Collection providers)
    {
        for (Iterator iterator = providers.iterator(); iterator.hasNext();)
        {
            SecurityProvider provider = (SecurityProvider) iterator.next();
            addProvider(provider);
        }
    }

    public SecurityContext createSecurityContext(Authentication authentication)
        throws UnknownAuthenticationTypeException
    {
        Iterator iter = providers.values().iterator();

        Class toTest = authentication.getClass();

        while (iter.hasNext())
        {
            SecurityProvider provider = (SecurityProvider) iter.next();

            if (provider.supports(toTest))
            {
                return provider.createSecurityContext(authentication);
            }
        }
        throw new UnknownAuthenticationTypeException(authentication);
    }

    public EncryptionStrategy getEncryptionStrategy(String name)
    {
        return (EncryptionStrategy) cryptoStrategies.get(name);
    }

    public void addEncryptionStrategy(EncryptionStrategy strategy)
    {
        cryptoStrategies.put(strategy.getName(), strategy);
    }

    public EncryptionStrategy removeEncryptionStrategy(String name)
    {
        return (EncryptionStrategy) cryptoStrategies.remove(name);

    }

    public Collection getEncryptionStrategies()
    {
        return Collections.unmodifiableCollection(new ArrayList(cryptoStrategies.values()));
    }

    public void setEncryptionStrategies(Collection strategies)
    {
        for (Iterator iterator = strategies.iterator(); iterator.hasNext();)
        {
            EncryptionStrategy strategy = (EncryptionStrategy) iterator.next();
            addEncryptionStrategy(strategy);
        }
    }

}
