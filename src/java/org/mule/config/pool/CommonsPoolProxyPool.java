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
 *
 */
package org.mule.config.pool;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleProxy;
import org.mule.umo.UMOException;
import org.mule.util.ObjectFactory;
import org.mule.util.ObjectPool;

import java.util.ArrayList;
import java.util.List;

/**
 * <code>CommonsPoolProxyPool</code> is pool used to store MuleProxy objects.
 * This pool is a jakarta commons-pool implementation.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CommonsPoolProxyPool implements ObjectPool
{
    /**
     * logger used by this class
     */
    protected static transient Log logger = LogFactory.getLog(CommonsPoolProxyPool.class);

    /**
     * The pool that holds the MuleProxy objects
     */
    private GenericObjectPool pool;

    /**
     * the factory used to create objects for the pool
     */
    private ObjectFactory factory;

    private List components;

    /**
     * Creates a new pool and an Object factory with the UMODescriptor
     * 
     * @param descriptor the descriptor to use when constructing MuleProxy
     *            objects in the pool
     */
    public CommonsPoolProxyPool(MuleDescriptor descriptor)
    {
        GenericObjectPool.Config config = new GenericObjectPool.Config();

       // if(descriptor.isSingleton()) {
            //config.maxIdle = 1;
            //config.maxActive = 1;
        //} else {
            config.maxIdle = descriptor.getPoolingProfile().getMaxIdle();
            config.maxActive = descriptor.getPoolingProfile().getMaxActive();
        //}
        config.maxWait = descriptor.getPoolingProfile().getMaxWait();
        config.whenExhaustedAction = (byte) descriptor.getPoolingProfile().getExhaustedAction();

        init(descriptor, config);

    }

    /**
     * @param descriptor the UMO descriptor to pool
     * @param config the config to use when configuring the pool
     */
    public CommonsPoolProxyPool(MuleDescriptor descriptor, GenericObjectPool.Config config)
    {
        init(descriptor, config);
    }

    /**
     * @param descriptor the UMO descriptor to pool
     * @param config the config to use when configuring the pool
     */
    private void init(MuleDescriptor descriptor, GenericObjectPool.Config config)
    {
        components = new ArrayList();
        setFactory(new CommonsPoolProxyFactory(descriptor, this));
        pool = new GenericObjectPool((PoolableObjectFactory) factory, config);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.model.pool.ObjectPool#borrowObject()
     */
    public Object borrowObject() throws Exception
    {
        return pool.borrowObject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.model.pool.ObjectPool#returnObject(java.lang.Object)
     */
    public void returnObject(Object object) throws Exception
    {
        pool.returnObject(object);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.model.pool.ObjectPool#getSize()
     */
    public int getSize()
    {
        return pool.getNumActive();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.model.pool.ObjectPool#getMaxSize()
     */
    public int getMaxSize()
    {
        return pool.getMaxActive();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.model.pool.ObjectPool#setFactory(org.mule.model.pool.ProxyFactory)
     */
    public void setFactory(ObjectFactory factory)
    {
        this.factory = factory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.model.pool.ObjectPool#clearPool()
     */
    public void clearPool()
    {
        synchronized (components) {
            MuleProxy proxy = null;
            for (int i = 0; i < components.size(); i++) {
                proxy = (MuleProxy) components.get(i);
                proxy.dispose();
            }
        }
        components.clear();
        pool.clear();
    }

    public void onAdd(Object proxy)
    {
        synchronized (components) {
            components.add(proxy);
        }
    }

    public void onRemove(Object proxy)
    {
        synchronized (components) {
            components.remove(proxy);
        }
    }

    public void start() throws UMOException
    {
        synchronized (components) {
            for (int i = 0; i < components.size(); i++) {
                ((MuleProxy) components.get(i)).start();
            }
        }
    }

    public void stop() throws UMOException
    {
        synchronized (components) {
            for (int i = 0; i < components.size(); i++) {
                ((MuleProxy) components.get(i)).stop();
            }
        }
    }

}
