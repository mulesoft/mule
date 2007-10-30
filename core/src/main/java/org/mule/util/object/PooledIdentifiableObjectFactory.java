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

import org.mule.config.ConfigurationException;
import org.mule.config.PoolingProfile;
import org.mule.config.i18n.MessageFactory;
import org.mule.umo.lifecycle.Disposable;
import org.mule.util.UUID;

import java.util.Map;

import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;

/**
 * Same as <code>PooledObjectFactory</code> but has a working <code>lookup(String id)</code> method for 
 * looking up a created object from the pool.  Note that the object's class must implement the 
 * <code>Identifiable</code> interface.
 * 
 * @see PooledObjectFactory
 * @see Identifiable
 */
public class PooledIdentifiableObjectFactory extends AbstractPooledObjectFactory implements KeyedPoolableObjectFactory
{
    /** 
     * Active instances of the object which have been created.  
     */
    private GenericKeyedObjectPool pool = null;
    
    /** For Spring only */
    public PooledIdentifiableObjectFactory() { super(); }
    
    public PooledIdentifiableObjectFactory(Class objectClass) { super(objectClass); }

    public PooledIdentifiableObjectFactory(Class objectClass, Map properties) { super(objectClass, properties); }
    
    public PooledIdentifiableObjectFactory(Class objectClass, PoolingProfile poolingProfile) { super(objectClass, poolingProfile); }

    public PooledIdentifiableObjectFactory(Class objectClass, Map properties, PoolingProfile poolingProfile) { super(objectClass, properties, poolingProfile); }
    
    protected void initialisePool() 
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
        Object obj = pool.borrowObject(UUID.getUUID());
        if ((obj instanceof Identifiable) == false)
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("Object " + obj + " does not implement the Identifiable interface.  PooledObjectFactory should be used for this object instead of PooledIdentifiableObjectFactory."));
        }        
        return obj;
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
        pool.returnObject(((Identifiable) object).getId(), object);
    }
    
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
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // KeyedPoolableObjectFactory Interface
    ////////////////////////////////////////////////////////////////////////////////////////////////

    public Object makeObject(Object key) throws Exception
    {
        Object obj = super.getOrCreate();
        ((Identifiable) obj).setId((String) key);
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
}
