/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.jndi;

import org.mule.runtime.core.api.MuleException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Defines a {@link JndiNameResolver} that uses a cache in order to store
 * the already resolved names.
 * <p/>
 * The cache does not have an automated mechanism for cleaning up the data.
 * In case of getting corrupt data, a way to cleaning up the cache is to stop
 * and then restart the instance.
 */
public class CachedJndiNameResolver extends AbstractJndiNameResolver
{

    protected Map<String, Object> cache;

    @Override
    public Object lookup(String name) throws NamingException
    {
        Object result = findInCache(name);

        if (result == null) {
            result = findInContext(name);
        }

        return result;
    }

    private Object findInContext(String name) throws NamingException
    {

        Context jndiContext = createInitialContext();

        try
        {
            Object result = jndiContext.lookup(name);

            if (result != null)
            {
                cache.put(name, result);
            }

            return result;
        }
        finally
        {
            jndiContext.close();
        }
    }

    private Object findInCache(String name)
    {
        Object result = null;
        if (name != null)
        {
            result = cache.get(name);
            if (logger.isDebugEnabled())
            {
                logger.debug(String.format("Object: " + name + " was %sfound in the cache", (result == null)? "not ": ""));
            }
        }

        return result;
    }

    @Override
    public void initialise() {
        cache = new ConcurrentHashMap<String, Object>();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Cleans up the cache.
     */
    @Override
    public void stop() throws MuleException
    {
        cache.clear();
    }
}
