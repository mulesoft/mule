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
 */
package org.mule.config.pool;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleProxy;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOManager;
import org.mule.util.ClassHelper;
import org.mule.util.ObjectFactory;
import org.mule.util.ObjectPool;

/**
 * <code>AbstractProxyFactory</code> provides common behaviour for creating proxy objects
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
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
                    throw new InitialisationException(new Message(Messages.NO_LOCAL_IMPL_X_SET_ON_DESCRIPTOR_X, refName, descriptor.getName()), this);
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
                        throw new InitialisationException(new Message(Messages.CANT_INSTANCIATE_NON_CONTAINER_REF_X, reference), e, descriptor);
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
