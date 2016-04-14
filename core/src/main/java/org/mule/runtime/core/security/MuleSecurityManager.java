/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.security;

import org.mule.api.EncryptionStrategy;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.api.security.Authentication;
import org.mule.api.security.SecurityContext;
import org.mule.api.security.SecurityException;
import org.mule.api.security.SecurityManager;
import org.mule.api.security.SecurityProvider;
import org.mule.api.security.SecurityProviderNotFoundException;
import org.mule.api.security.UnauthorisedException;
import org.mule.api.security.UnknownAuthenticationTypeException;
import org.mule.config.i18n.CoreMessages;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    private Map<String, SecurityProvider> providers = new ConcurrentHashMap<String, SecurityProvider>();
    private Map<String, EncryptionStrategy> cryptoStrategies = new ConcurrentHashMap<String, EncryptionStrategy>();

    public MuleSecurityManager()
    {
        super();
    }

    @Override
    public void initialise() throws InitialisationException
    {
        List<Initialisable> all = new LinkedList<Initialisable>(providers.values());
        // ordering: appends
        all.addAll(cryptoStrategies.values());
        LifecycleTransitionResult.initialiseAll(all.iterator());
    }

    @Override
    public Authentication authenticate(Authentication authentication)
        throws SecurityException, SecurityProviderNotFoundException
    {
        Iterator<SecurityProvider> iter = providers.values().iterator();
        Class<? extends Authentication> toTest = authentication.getClass();

        while (iter.hasNext())
        {
            SecurityProvider provider = iter.next();

            if (provider.supports(toTest))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Authentication attempt using " + provider.getClass().getName());
                }

                Authentication result = null;
                try
                {
                    result = provider.authenticate(authentication);
                }
                catch (Exception e)
                {
                    if (!iter.hasNext())
                    {
                        throw new UnauthorisedException(CoreMessages.authorizationAttemptFailed(), e);
                    }
                }

                if (result != null)
                {
                    return result;
                }
            }
        }

        throw new SecurityProviderNotFoundException(toTest.getName());
    }

    @Override
    public void addProvider(SecurityProvider provider)
    {
        if (getProvider(provider.getName()) != null)
        {
            throw new IllegalArgumentException("Provider already registered: " + provider.getName());
        }
        providers.put(provider.getName(), provider);
    }

    @Override
    public SecurityProvider getProvider(String name)
    {
        if (name == null)
        {
            throw new IllegalArgumentException("provider Name cannot be null");
        }
        return providers.get(name);
    }

    @Override
    public SecurityProvider removeProvider(String name)
    {
        return providers.remove(name);
    }

    @Override
    public Collection<SecurityProvider> getProviders()
    {
        ArrayList<SecurityProvider> providersList = new ArrayList<SecurityProvider>(providers.values());
        return Collections.unmodifiableCollection(providersList);
    }

    @Override
    public void setProviders(Collection<SecurityProvider> providers)
    {
        for (SecurityProvider provider : providers)
        {
            addProvider(provider);
        }
    }

    @Override
    public SecurityContext createSecurityContext(Authentication authentication)
        throws UnknownAuthenticationTypeException
    {
        Iterator<SecurityProvider> iter = providers.values().iterator();
        Class<? extends Authentication> toTest = authentication.getClass();

        while (iter.hasNext())
        {
            SecurityProvider provider = iter.next();
            if (provider.supports(toTest))
            {
                return provider.createSecurityContext(authentication);
            }
        }
        throw new UnknownAuthenticationTypeException(authentication);
    }

    @Override
    public EncryptionStrategy getEncryptionStrategy(String name)
    {
        return cryptoStrategies.get(name);
    }

    @Override
    public void addEncryptionStrategy(EncryptionStrategy strategy)
    {
        cryptoStrategies.put(strategy.getName(), strategy);
    }

    @Override
    public EncryptionStrategy removeEncryptionStrategy(String name)
    {
        return cryptoStrategies.remove(name);
    }

    @Override
    public Collection<EncryptionStrategy> getEncryptionStrategies()
    {
        List<EncryptionStrategy> allStrategies = new ArrayList<EncryptionStrategy>(cryptoStrategies.values());
        return Collections.unmodifiableCollection(allStrategies);
    }

    @Override
    public void setEncryptionStrategies(Collection<EncryptionStrategy> strategies)
    {
        for (EncryptionStrategy strategy : strategies)
        {
            addEncryptionStrategy(strategy);
        }
    }
}
