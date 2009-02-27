/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.pool;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.object.ObjectFactory;
import org.mule.config.PoolingProfile;
import org.mule.config.i18n.MessageFactory;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * <code>CommonsPoolProxyPool</code> is an implementation of {@link ObjectPool}
 * that internally uses the commons-pool {@link GenericObjectPool} and uses a
 * {@link ObjectFactory} for creating new pooled instances.
 */
public class CommonsPoolObjectPool implements ObjectPool
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(CommonsPoolObjectPool.class);

    /**
     * The pool
     */
    protected GenericObjectPool pool;

    /**
     * The ObjectFactory used to create new pool instances
     */
    protected ObjectFactory objectFactory;

    /**
     * The pooling profile used to configure and initialise pool
     */
    protected PoolingProfile poolingProfile;

    /**
     * Creates a new pool and an Object factory with the ServiceDescriptor
     * 
     * @param descriptor the descriptor to use when constructing MuleProxy objects in
     *            the pool
     */
    public CommonsPoolObjectPool(ObjectFactory objectFactory, PoolingProfile poolingProfile)
    {
        this.objectFactory = objectFactory;
        this.poolingProfile = poolingProfile;
    }

    public void initialise() throws InitialisationException
    {
        GenericObjectPool.Config config = new GenericObjectPool.Config();

        if (poolingProfile != null)
        {
            config.maxIdle = poolingProfile.getMaxIdle();
            config.maxActive = poolingProfile.getMaxActive();
            config.maxWait = poolingProfile.getMaxWait();
            config.whenExhaustedAction = (byte) poolingProfile.getExhaustedAction();
        }

        pool = new GenericObjectPool(getPooledObjectFactory(), config);

        try
        {
            applyInitialisationPolicy();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * Template method to be overridden by implementations that do more than just
     * invoke objectFactory
     * 
     * @return
     */
    protected PoolableObjectFactory getPooledObjectFactory()
    {
        return new PoolabeObjectFactoryAdaptor();
    }

    protected void applyInitialisationPolicy() throws Exception
    {
        if (poolingProfile != null)
        {
            int numToBorrow = 0;
            int initPolicy = poolingProfile.getInitialisationPolicy();

            if (initPolicy == PoolingProfile.INITIALISE_ALL)
            {
                numToBorrow = poolingProfile.getMaxActive();
            }
            else if (initPolicy == PoolingProfile.INITIALISE_ONE)
            {
                numToBorrow = 1;
            }

            List holderList = new ArrayList(numToBorrow);
            try
            {
                for (int t = 0; t < numToBorrow; t++)
                {
                    holderList.add(getPooledObjectFactory().makeObject());
                }
            }
            finally
            {
                for (int t = 0; t < holderList.size(); t++)
                {
                    Object obj = holderList.get(t);
                    if (obj != null)
                    {
                        this.returnObject(obj);
                    }
                }
            }
        }
    }

    public Object borrowObject() throws Exception
    {
        if (pool != null)
        {
            return pool.borrowObject();
        }
        else
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Object pool has not been initialized."), this);
        }
    }

    public void returnObject(Object object)
    {
        if (pool != null)
        {
            try
            {
                pool.returnObject(object);
            }
            catch (Exception ex)
            {
                // declared Exception is never thrown from pool; this is a known bug
                // in
                // the pool API
            }
        }
    }

    public int getNumActive()
    {
        return pool.getNumActive();
    }

    public int getMaxActive()
    {
        return pool.getMaxActive();
    }

    public void dispose()
    {
        if (pool != null)
        {
            try
            {
                pool.close();
            }
            catch (Exception e)
            {
                // close() never throws - wrong method signature
            }
            finally
            {
                pool = null;
            }
        }
    }

    public void clear()
    {
        if (pool != null)
        {
            pool.clear();
        }

    }

    public void close()
    {
        if (pool != null)
        {
            try
            {
                pool.close();
            }
            catch (Exception e)
            {
                // close() never throws - wrong method signature
            }
            finally
            {
                pool = null;
            }
        }

    }

    public void setObjectFactory(ObjectFactory objectFactory)
    {
        this.objectFactory = objectFactory;
    }

    public ObjectFactory getObjectFactory()
    {
        return objectFactory;
    }

    /**
     * Wraps org.mule.object.ObjectFactory with commons-pool PoolableObjectFactory
     */
    class PoolabeObjectFactoryAdaptor implements PoolableObjectFactory
    {

        public void activateObject(Object obj) throws Exception
        {
            // nothing to do
        }

        public void destroyObject(Object obj) throws Exception
        {
            if (obj instanceof Disposable)
            {
                ((Disposable) obj).dispose();
            }
        }

        public Object makeObject() throws Exception
        {
            return objectFactory.getInstance();
        }

        public void passivateObject(Object obj) throws Exception
        {
            // nothing to do
        }

        public boolean validateObject(Object obj)
        {
            return true;
        }

    }

}
