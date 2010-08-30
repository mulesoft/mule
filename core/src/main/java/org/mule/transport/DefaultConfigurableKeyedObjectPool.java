/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import java.util.NoSuchElementException;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * Implements {@link ConfigurableKeyedObjectPool} as a delegate of a {@link KeyedPoolableObjectFactory}
 * instance.
 */
public class DefaultConfigurableKeyedObjectPool implements ConfigurableKeyedObjectPool
{

    private final GenericKeyedObjectPool pool;

    public DefaultConfigurableKeyedObjectPool()
    {
        pool = new GenericKeyedObjectPool();

        // NOTE: testOnBorrow MUST be FALSE. this is a bit of a design bug in
        // commons-pool since validate is used for both activation and passivation,
        // but has no way of knowing which way it is going.
        pool.setTestOnBorrow(false);
        pool.setTestOnReturn(true);
    }

    @Override
    public Object borrowObject(Object key) throws Exception, NoSuchElementException, IllegalStateException
    {
        return pool.borrowObject(key);
    }

    @Override
    public void returnObject(Object key, Object obj) throws Exception
    {
        pool.returnObject(key, obj);
    }

    @Override
    public void invalidateObject(Object key, Object obj) throws Exception
    {
        pool.invalidateObject(key, obj);
    }

    @Override
    public void addObject(Object key) throws Exception, IllegalStateException, UnsupportedOperationException
    {
        pool.addObject(key);
    }

    @Override
    public int getNumIdle(Object key) throws UnsupportedOperationException
    {
        return pool.getNumIdle(key);
    }

    @Override
    public int getNumActive(Object key) throws UnsupportedOperationException
    {
        return pool.getNumActive(key);
    }

    @Override
    public int getNumIdle() throws UnsupportedOperationException
    {
        return pool.getNumIdle();
    }

    @Override
    public int getNumActive() throws UnsupportedOperationException
    {
        return pool.getNumActive();
    }

    @Override
    public void clear()
    {
        pool.clear();
    }

    @Override
    public void clear(Object key) throws Exception, UnsupportedOperationException
    {
        pool.clear(key);
    }

    @Override
    public void close() throws Exception
    {
        pool.close();
    }

    @Override
    public void setFactory(KeyedPoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException
    {
        pool.setFactory(factory);
    }

    @Override
    public int getMaxActive()
    {
        return pool.getMaxActive();
    }

    @Override
    public int getMaxTotal()
    {
        return pool.getMaxTotal();
    }

    @Override
    public void setMaxWait(long maxWait)
    {
        pool.setMaxWait(maxWait);
    }

    @Override
    public void setMaxActive(int maxActive)
    {
        pool.setMaxActive(maxActive);
    }

    @Override
    public void setMaxIdle(int maxIdle)
    {
        pool.setMaxIdle(maxIdle);
    }

    @Override
    public void setMaxTotal(int maxTotal)
    {
        pool.setMaxTotal(maxTotal);
    }

    @Override
    public int getMaxIdle()
    {
        return pool.getMaxIdle();
    }

    @Override
    public void setWhenExhaustedAction(byte whenExhaustedAction)
    {
        pool.setWhenExhaustedAction(whenExhaustedAction);
    }

    @Override
    public byte getWhenExhaustedAction()
    {
        return pool.getWhenExhaustedAction();
    }

    @Override
    public long getMaxWait()
    {
        return pool.getMaxWait();
    }
}
