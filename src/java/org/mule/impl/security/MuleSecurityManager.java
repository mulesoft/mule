/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.security;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.InitialisationException;
import org.mule.umo.security.SecurityProviderNotFoundException;
import org.mule.umo.security.UMOAuthentication;
import org.mule.umo.security.UMOSecurityContext;
import org.mule.umo.security.UMOSecurityContextFactory;
import org.mule.umo.security.UMOSecurityException;
import org.mule.umo.security.UMOSecurityManager;
import org.mule.umo.security.UMOSecurityProvider;
import org.mule.umo.security.UnknownAuthenticationTypeException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>MuleSecurityManager</code> is a default implementation security manager
 * for a Mule instance
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class MuleSecurityManager implements UMOSecurityManager
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleSecurityManager.class);

    private Map providers = new ConcurrentHashMap();

    private Map contextFactories = new ConcurrentHashMap();

    public void initialise() throws InitialisationException
    {
        for (Iterator iterator = providers.values().iterator(); iterator.hasNext();)
        {
            UMOSecurityProvider provider = (UMOSecurityProvider) iterator.next();
            provider.initialise();
        }
    }

    public UMOAuthentication authenticate(UMOAuthentication authentication) throws UMOSecurityException
    {
        Iterator iter = providers.values().iterator();

        Class toTest = authentication.getClass();

        while (iter.hasNext()) {
            UMOSecurityProvider provider = (UMOSecurityProvider) iter.next();

            if (provider.supports(toTest)) {
                logger.debug("Authentication attempt using "
                    + provider.getClass().getName());

                UMOAuthentication result = provider.authenticate(authentication);

                if (result != null) {
                    return result;
                }
            }
        }

        throw new SecurityProviderNotFoundException("No authentication provider for "
            + toTest.getName());
    }

    public void addProvider(UMOSecurityProvider provider)
    {
        if(getProvider(provider.getName())!=null) {
            throw new IllegalArgumentException("Provider already registered: " + provider.getName());
        }
        providers.put(provider.getName(), provider);
    }

    public UMOSecurityProvider getProvider(String name)
    {
        if(name==null) throw new NullPointerException("provider Name cannot be null");
        return (UMOSecurityProvider)providers.get(name);
    }

    public UMOSecurityProvider removeProvider(String name)
    {
        return (UMOSecurityProvider)providers.remove(name);
    }

    public List getProviders()
    {
        return Collections.unmodifiableList(new ArrayList(providers.values()));
    }

    public void setProviders(List providers)
    {
        for (Iterator iterator = providers.iterator(); iterator.hasNext();)
        {
            UMOSecurityProvider provider = (UMOSecurityProvider) iterator.next();
            addProvider(provider);
        }
    }

    public UMOSecurityContext createSecurityContext(UMOAuthentication auth) throws UnknownAuthenticationTypeException
    {
        UMOSecurityContextFactory factory = (UMOSecurityContextFactory)contextFactories.get(auth.getClass());
        if(factory==null) {
            throw new UnknownAuthenticationTypeException(auth);
        } else {
            return factory.create(auth);
        }
    }

    public void addSecurityContextFactory(Class type, UMOSecurityContextFactory factory)
    {
        if(contextFactories.get(type)!=null) {
            throw new IllegalArgumentException("Factory for type already registered: " + type.getName());
        }
        contextFactories.put(type, factory);
    }

    public UMOSecurityContextFactory removeSecurityContextFactory(Class type)
    {
        return (UMOSecurityContextFactory)contextFactories.remove(type);
    }
}
