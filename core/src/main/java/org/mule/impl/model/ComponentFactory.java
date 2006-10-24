/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.MuleManager;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.container.ContainerKeyPair;
import org.mule.umo.UMOException;
import org.mule.umo.UMODescriptor;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.manager.UMOManager;
import org.mule.util.BeanUtils;

/**
 * Reusable methods for working with UMOComponents.
 */
public class ComponentFactory
{

    /**
     * Creates a component based on its descriptor.
     * 
     * @param descriptor the descriptor to create the component from
     * @return The newly created component
     * @throws UMOException
     */
    public static Object createComponent(UMODescriptor descriptor) throws UMOException
    {
        UMOManager manager = MuleManager.getInstance();
        Object impl = descriptor.getImplementation();
        Object component;

        if (impl instanceof String)
        {
            impl = new ContainerKeyPair(null, impl);
        }
        if (impl instanceof ContainerKeyPair)
        {
            component = manager.getContainerContext().getComponent(impl);

            if (descriptor.isSingleton())
            {
                descriptor.setImplementation(component);
            }
        }
        else
        {
            component = impl;
        }

        try
        {
            BeanUtils.populate(component, descriptor.getProperties());
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_TO_SET_PROPERTIES_ON_X,
                "Component '" + descriptor.getName() + "'"), e, descriptor);
        }

        // Call any custom initialisers
        if (descriptor instanceof MuleDescriptor)
        {
            ((MuleDescriptor)descriptor).fireInitialisationCallbacks(component);
        }

        return component;
    }
}
