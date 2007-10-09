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

import org.mule.config.PoolingProfile;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.UUID;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * Creates a new instance of the object on each call.  If the object implements the Identifiable 
 * interface, individual instances can be looked up by ID.
 */
public class PooledObjectFactory extends AbstractObjectFactory implements KeyedPoolableObjectFactory
{
    /** 
     * Active instances of the object which have been created.  
     */
    protected GenericKeyedObjectPool pool = null;
    
    protected PoolingProfile poolingProfile = null;
    
    /** For Spring only */
    public PooledObjectFactory() { super(); }
    
    public PooledObjectFactory(Class objectClass) { super(objectClass); }

    public PooledObjectFactory(Class objectClass, Map properties) { super(objectClass, properties); }
    
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
    
    public void initialise() throws InitialisationException
    {
        GenericKeyedObjectPool.Config config = new GenericKeyedObjectPool.Config();
        if (poolingProfile != null)
        {
            config.maxIdle = poolingProfile.getMaxIdle();
            config.maxActive = poolingProfile.getMaxActive();
            config.maxWait = poolingProfile.getMaxWait();
            config.whenExhaustedAction = (byte) poolingProfile.getExhaustedAction();
        }
        pool = new GenericKeyedObjectPool(this, config);

        try
        {
            applyInitialisationPolicy();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
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
                    holderList.add(this.getOrCreate());
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

    public void dispose()
    {
        if (pool != null)
        {
            try 
            {
                // TODO Ideally we should call Disposable.dispose() on each object in the pool before destroying it.
                pool.close();
            }
            catch (Exception e)
            {
                logger.warn(e);
            }
            finally
            {
                pool = null;
            }
        }
    }

    /**
     * Creates a new instance of the object on each call.
     */
    public Object getOrCreate() throws Exception
    {
        return pool.borrowObject(UUID.getUUID());
    }

    /** {@inheritDoc} */
    public Object lookup(String id) throws Exception
    {
        return pool.borrowObject(id);
    }

    /** 
     * Returns the object instance to the pool.
     */
    public void release(Object object) throws Exception
    {
        if (object instanceof Identifiable)
        {
            pool.returnObject(((Identifiable) object).getId(), object);
        }
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // KeyedPoolableObjectFactory Interface
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public Object makeObject(Object key) throws Exception
    {
        Object obj = super.getOrCreate();
        if (obj instanceof Identifiable)
        {
            ((Identifiable) obj).setId((String) key);
        }
        return obj;
    }

    public void destroyObject(Object key, Object obj) throws Exception
    {
        if (obj instanceof Disposable)
        {
            ((Disposable) obj).dispose();
        }
    }

    public void activateObject(Object key, Object obj) throws Exception
    {
        // nothing to do
    }

    public void passivateObject(Object key, Object obj) throws Exception
    {
        // nothing to do
    }

    public boolean validateObject(Object key, Object obj)
    {
        return true;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public PoolingProfile getPoolingProfile()
    {
        return poolingProfile;
    }

    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }
}
