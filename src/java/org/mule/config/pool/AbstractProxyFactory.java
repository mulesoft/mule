/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.config.pool;

import org.mule.InitialisationException;
import org.mule.MuleManager;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleProxy;
import org.mule.umo.UMOException;
import org.mule.umo.UMOManager;
import org.mule.util.ClassHelper;
import org.mule.util.ObjectFactory;
import org.mule.util.ObjectPool;

/**
 * <code>AbstractProxyFactory</code> provides common behaviour for creating proxy objects
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractProxyFactory implements ObjectFactory
{
    /**
     * The UMODescriptor used to create new components in the pool
     */
    protected MuleDescriptor descriptor;
    protected ObjectPool pool;

    /**
     * Creates a pool factory using the descriptor as the basis for creating its objects
     *
     * @param descriptor the descriptor to use to construct a MuleProxy
     * @see org.mule.umo.UMODescriptor
     */
    public AbstractProxyFactory(MuleDescriptor descriptor, ObjectPool pool)
    {
        this.descriptor = descriptor;
        this.pool = pool;
    }

    public Object create() throws UMOException
    {
        UMOManager manager = MuleManager.getInstance();
        Object impl = descriptor.getImplementation();
        Object component = null;

        if (impl instanceof String)
        {
            String reference = impl.toString();

            if (reference.startsWith(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL))
            {
                String refName = reference.substring(MuleDescriptor.IMPLEMENTATION_TYPE_LOCAL.length());
                component = descriptor.getProperties().get(refName);
                if(component==null) {
                    throw new InitialisationException("Component implementation type is 'local' but no property called "
                            + refName + " is set on the descriptor called " + descriptor.getName());
                }
            }

            if (component == null)
            {
                if (descriptor.isContainerManaged())
                {
                    component = manager.getContainerContext().getComponent(reference);
                } else
                {
                    try
                    {
                        component = ClassHelper.instanciateClass(reference, new Object[]{});
                    } catch (Exception e)
                    {
                        throw new InitialisationException("Failed to instanciate non-container managed object reference: " + reference + ". " + e.getMessage(), e);
                    }
                }
            }
        } else {
            component = impl;
        }
        //Call any custom initialisers
        descriptor.fireInitialisationCallbacks(component);

        afterComponentCreate(component);

        MuleProxy proxy = new MuleProxy(component, descriptor, pool);
        return proxy;
    }

    protected void afterComponentCreate(Object component) throws InitialisationException
    {

    }
}
