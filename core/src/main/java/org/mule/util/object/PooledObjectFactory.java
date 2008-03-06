/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.LifecycleTransitionResult;
import org.mule.config.PoolingProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;

/**
 * Creates a new instance of the object on each call. If the object implements the
 * Identifiable interface, individual instances can be looked up by ID.
 */
public class PooledObjectFactory extends AbstractObjectFactory implements PoolableObjectFactory
{
    /**
     * Active instances of the object which have been created.
     */
    protected GenericObjectPool pool = null;

    protected PoolingProfile poolingProfile = null;

    /** For Spring only */
    public PooledObjectFactory()
    {
        super();
    }

    public PooledObjectFactory(Class objectClass)
    {
        super(objectClass);
    }

    public PooledObjectFactory(Class objectClass, Map properties)
    {
        super(objectClass, properties);
    }

    public PooledObjectFactory(Class objectClass, PoolingProfile poolingProfile)
    {
        super(objectClass);
        this.poolingProfile = poolingProfile;
    }

    public PooledObjectFactory(Class objectClass, Map properties, PoolingProfile poolingProfile)
    {
        super(objectClass, properties);
        this.poolingProfile = poolingProfile;
    }

    // @Override
    public LifecycleTransitionResult initialise() throws InitialisationException
    {
        super.initialise();

        GenericObjectPool.Config config = new GenericObjectPool.Config();

        if (poolingProfile != null)
        {
            config.maxIdle = poolingProfile.getMaxIdle();
            config.maxActive = poolingProfile.getMaxActive();
            config.maxWait = poolingProfile.getMaxWait();
            config.whenExhaustedAction = (byte) poolingProfile.getExhaustedAction();
        }

        pool = new GenericObjectPool(this, config);

        try
        {
            applyInitialisationPolicy();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }

        return LifecycleTransitionResult.OK;
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
                    holderList.add(this.getInstance());
                }
            }
            finally
            {
                for (int t = 0; t < holderList.size(); t++)
                {
                    Object obj = holderList.get(t);
                    if (obj != null)
                    {
                        this.release(obj);
                    }
                }
            }
        }
    }

    // @Override
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

    /**
     * Returns an instance from the internal object pool.
     */
    // @Override
    public Object getInstance() throws Exception
    {
        return pool.borrowObject();
    }

    /**
     * Returns the given object instance to the pool.
     */
    // @Override
    public void release(Object object)
    {
        try
        {
            pool.returnObject(object);
        }
        catch (Exception ex)
        {
            // declared Exception is never thrown from pool; this is a bug in the method signature
        }
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////
    // PoolableObjectFactory Interface
    // //////////////////////////////////////////////////////////////////////////////////////////////

    public Object makeObject() throws Exception
    {
        return super.getInstance();
    }

    public void destroyObject(Object obj) throws Exception
    {
        if (obj instanceof Disposable)
        {
            ((Disposable) obj).dispose();
        }
    }

    public void activateObject(Object obj) throws Exception
    {
        // nothing to do
    }

    public void passivateObject(Object obj) throws Exception
    {
        // nothing to do
    }

    public boolean validateObject(Object obj)
    {
        return true;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    // //////////////////////////////////////////////////////////////////////////////////////////////

    public int getPoolSize()
    {
        if (pool != null)
        {
            return pool.getNumActive();
        }
        else
        {
            return 0;
        }
    }

    public PoolingProfile getPoolingProfile()
    {
        return poolingProfile;
    }

    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }
}
