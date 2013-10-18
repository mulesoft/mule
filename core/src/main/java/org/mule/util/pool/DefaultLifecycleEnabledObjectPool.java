/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.pool;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.JavaComponent;
import org.mule.api.component.LifecycleAdapter;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.object.ObjectFactory;
import org.mule.component.PooledJavaComponent;
import org.mule.config.PoolingProfile;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * A LifecyleEnabledObjectPool implementation for pooling {@link LifecycleAdapter}
 * instances for implementations of {@link JavaComponent} that require
 * {@link LifecycleAdapter} pooling such as {@link PooledJavaComponent}.
 * 
 * @see PooledJavaComponent
 */
public class DefaultLifecycleEnabledObjectPool extends CommonsPoolObjectPool implements LifecyleEnabledObjectPool
{
    /**
     * logger used by this class
     */
    protected static final Log logger = LogFactory.getLog(DefaultLifecycleEnabledObjectPool.class);

    protected AtomicBoolean started = new AtomicBoolean(false);

    private List items = new LinkedList();

    /**
     * @param objectFactory The object factory that should be used to create new
     *            {@link org.mule.api.component.LifecycleAdapter} instance for the pool
     * @param poolingProfile The pooling progile ot be used to configure pool
     * @param muleContext
     */
    public DefaultLifecycleEnabledObjectPool(ObjectFactory objectFactory, PoolingProfile poolingProfile, MuleContext muleContext)
    {
        super(objectFactory, poolingProfile, muleContext);
    }

    protected PoolableObjectFactory getPooledObjectFactory()
    {
        return new LifecycleEnabledPoolabeObjectFactoryAdapter();
    }

    public void start() throws MuleException
    {
        started.set(true);
        synchronized (items)
        {
            for (Iterator i = items.iterator(); i.hasNext();)
            {
                ((Startable) i.next()).start();
            }
        }
    }

    public void stop() throws MuleException
    {
        started.set(false);
        synchronized (items)
        {
            for (Iterator i = items.iterator(); i.hasNext();)
            {
                ((Stoppable) i.next()).stop();
            }
        }
    }

    /**
     * Wraps org.mule.object.ObjectFactory with commons-pool PoolableObjectFactory
     */
    class LifecycleEnabledPoolabeObjectFactoryAdapter implements PoolableObjectFactory
    {

        public void activateObject(Object obj) throws Exception
        {
            // nothing to do
        }

        public void destroyObject(Object obj) throws Exception
        {
            // Only stop existing objects if they havn't already been stopped
            if (started.get() && obj instanceof Stoppable)
            {
                ((Stoppable) obj).stop();
            }
            if (obj instanceof Disposable)
            {
                ((Disposable) obj).dispose();
            }
            synchronized (items)
            {
                items.remove(obj);
            }
        }

        public Object makeObject() throws Exception
        {
            Object object = objectFactory.getInstance(muleContext);
            // Only start newly created objects if pool is started
            if (started.get() && object instanceof Startable)
            {
                ((Startable) object).start();
            }
            synchronized (items)
            {
                items.add(object);
            }
            return object;
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
