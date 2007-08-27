/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.pool;

import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.ComponentFactory;
import org.mule.impl.model.DefaultMuleProxy;
import org.mule.tck.testmodels.mule.TestMuleProxy;
import org.mule.tck.testmodels.mule.TestSedaModel;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOModel;
import org.mule.util.object.ObjectFactory;
import org.mule.util.object.ObjectPool;

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
    protected UMOModel model;
    protected ObjectPool pool;

    /**
     * Creates a pool factory using the descriptor as the basis for creating its
     * objects
     * 
     * @param descriptor the descriptor to use to construct a MuleProxy
     * @see org.mule.umo.UMODescriptor
     */
    public AbstractProxyFactory(MuleDescriptor descriptor, UMOModel model)
    {
        this.descriptor = descriptor;
        this.model = model;
    }

    public Object create() throws UMOException
    {
        Object component = ComponentFactory.createService(descriptor);
        afterComponentCreate(component);
        return createProxy(component);
    }

    protected Object createProxy(Object component) throws UMOException
    {
        if (model instanceof TestSedaModel)
        {
            return new TestMuleProxy(component, descriptor, model, pool);
        }
        else 
        {
            return new DefaultMuleProxy(component, descriptor, model, pool);
        }
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
