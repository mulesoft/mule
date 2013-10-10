/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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

    public Object borrowObject(Object key) throws Exception, NoSuchElementException, IllegalStateException
    {
        return pool.borrowObject(key);
    }

    public void returnObject(Object key, Object obj) throws Exception
    {
        pool.returnObject(key, obj);
    }

    public void invalidateObject(Object key, Object obj) throws Exception
    {
        pool.invalidateObject(key, obj);
    }

    public void addObject(Object key) throws Exception, IllegalStateException, UnsupportedOperationException
    {
        pool.addObject(key);
    }

    public int getNumIdle(Object key) throws UnsupportedOperationException
    {
        return pool.getNumIdle(key);
    }

    public int getNumActive(Object key) throws UnsupportedOperationException
    {
        return pool.getNumActive(key);
    }

    public int getNumIdle() throws UnsupportedOperationException
    {
        return pool.getNumIdle();
    }

    public int getNumActive() throws UnsupportedOperationException
    {
        return pool.getNumActive();
    }

    public void clear()
    {
        pool.clear();
    }

    public void clear(Object key) throws Exception, UnsupportedOperationException
    {
        pool.clear(key);
    }

    public void close() throws Exception
    {
        pool.close();
    }

    public void setFactory(KeyedPoolableObjectFactory factory) throws IllegalStateException, UnsupportedOperationException
    {
        pool.setFactory(factory);
    }

    public int getMaxActive()
    {
        return pool.getMaxActive();
    }

    public int getMaxTotal()
    {
        return pool.getMaxTotal();
    }

    public void setMaxWait(long maxWait)
    {
        pool.setMaxWait(maxWait);
    }

    public void setMaxActive(int maxActive)
    {
        pool.setMaxActive(maxActive);
    }

    public void setMaxIdle(int maxIdle)
    {
        pool.setMaxIdle(maxIdle);
    }

    public void setMaxTotal(int maxTotal)
    {
        pool.setMaxTotal(maxTotal);
    }

    public int getMaxIdle()
    {
        return pool.getMaxIdle();
    }

    public void setWhenExhaustedAction(byte whenExhaustedAction)
    {
        pool.setWhenExhaustedAction(whenExhaustedAction);
    }

    public byte getWhenExhaustedAction()
    {
        return pool.getWhenExhaustedAction();
    }

    public long getMaxWait()
    {
        return pool.getMaxWait();
    }
}
