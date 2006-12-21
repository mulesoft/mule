/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.pool;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.ComponentFactory;
import org.mule.impl.model.DefaultMuleProxy;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ObjectFactory;
import org.mule.util.ObjectPool;

/**
 * <code>AbstractProxyFactory</code> provides common behaviour for creating proxy
 * objects.
 */

public abstract class AbstractProxyFactory implements ObjectFactory
{
    /**
     * The UMODescriptor used to create new components in the pool
     */
    protected MuleDescriptor descriptor;
    protected ObjectPool pool;

    /**
     * Creates a pool factory using the descriptor as the basis for creating its
     * objects
     * 
     * @param descriptor the descriptor to use to construct a MuleProxy
     * @see org.mule.umo.UMODescriptor
     */
    public AbstractProxyFactory(MuleDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    public Object create() throws UMOException
    {
        Object component = ComponentFactory.createComponent(descriptor);
        afterComponentCreate(component);
        return createProxy(component);
    }

    protected Object createProxy(Object component) throws UMOException
    {
        return new DefaultMuleProxy(component, descriptor, pool);
    }

    protected void afterComponentCreate(Object component) throws InitialisationException
    {
        // nothing to do
    }

    public ObjectPool getPool()
    {
        return pool;
    }

    public void setPool(ObjectPool pool)
    {
        this.pool = pool;
    }
}
