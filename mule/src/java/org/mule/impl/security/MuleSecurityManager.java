/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.security;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.umo.UMOEncryptionStrategy;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.RecoverableException;
import org.mule.umo.security.SecurityException;
import org.mule.umo.security.*;

import java.util.*;

/**
 * <code>MuleSecurityManager</code> is a default implementation security manager
 * for a Mule instance
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class MuleSecurityManager implements UMOSecurityManager
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(MuleSecurityManager.class);

    private Map providers = new ConcurrentHashMap();

    private Map cryptoStrategies = new ConcurrentHashMap();

    public void initialise() throws InitialisationException, RecoverableException
    {
        for (Iterator iterator = providers.values().iterator(); iterator.hasNext();)
        {
            UMOSecurityProvider provider = (UMOSecurityProvider) iterator.next();
            provider.initialise();
        }

        for (Iterator iterator = cryptoStrategies.values().iterator(); iterator.hasNext();)
        {
            UMOEncryptionStrategy strategy = (UMOEncryptionStrategy) iterator.next();
            strategy.initialise();
        }
    }

    public UMOAuthentication authenticate(UMOAuthentication authentication) throws SecurityException, SecurityProviderNotFoundException
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

        throw new SecurityProviderNotFoundException(toTest.getName());
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

    public UMOSecurityContext createSecurityContext(UMOAuthentication authentication) throws UnknownAuthenticationTypeException
    {
        Iterator iter = providers.values().iterator();

        Class toTest = authentication.getClass();

        while (iter.hasNext()) {
            UMOSecurityProvider provider = (UMOSecurityProvider) iter.next();

            if (provider.supports(toTest)) {
                return provider.createSecurityContext(authentication);
            }
        }
        throw new UnknownAuthenticationTypeException(authentication);
    }

    public UMOEncryptionStrategy getEncryptionStrategy(String name)
    {
        return (UMOEncryptionStrategy)cryptoStrategies.get(name);
    }

    public void addEncryptionStrategy(String name, UMOEncryptionStrategy strategy)
    {
        cryptoStrategies.put(name, strategy);
    }

    public UMOEncryptionStrategy removeEncryptionStrategy(String name)
    {
        return (UMOEncryptionStrategy)cryptoStrategies.remove(name);

    }

    public void setEncryptionStrategies(Map strategies)
    {
        cryptoStrategies.putAll(strategies);
    }
}
