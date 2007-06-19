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

import org.mule.config.i18n.MessageFactory;
import org.mule.impl.MuleDescriptor;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.util.BeanUtils;

/**
 * Reusable methods for working with UMOComponents.
 */
public final class ComponentFactory
{

    /** Do not instanciate. */
    private ComponentFactory ()
    {
        // no-op
    }

    /**
     * Creates a component based on its descriptor.
     * 
     * @param descriptor the descriptor to create the component from
     * @return The newly created component
     * @throws UMOException
     */
    public static Object createService(UMODescriptor descriptor) throws UMOException
    {
        Object component;
        try
        {
            component = descriptor.getServiceFactory().create();
            
            // TODO MULE-1933 Would be nice to remove this eventually.
            BeanUtils.populate(component, descriptor.getProperties());
        }
        catch (Exception e)
        {
            throw new LifecycleException(MessageFactory.createStaticMessage("Unable to create component"), e);
        }

        // Call any custom initialisers
        if (descriptor instanceof MuleDescriptor)
        {
            ((MuleDescriptor) descriptor).fireInitialisationCallbacks(component);
        }

        return component;
    }
}
