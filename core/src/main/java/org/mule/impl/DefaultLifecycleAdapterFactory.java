/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.UMOLifecycleAdapter;
import org.mule.umo.lifecycle.UMOLifecycleAdapterFactory;
import org.mule.umo.model.UMOEntryPointResolver;

/**
 * <code>DefaultLifecycleAdapterFactory</code> creates a DefaultLifeCycleAdapter.  Users can
 * implement their own LifeCycleAdapter factories to control lifecycle events on their services such
 * as introduce other lifecycle events that are controlled by external changes.
 *
 * @see org.mule.umo.lifecycle.UMOLifecycleAdapter
 * @see org.mule.umo.lifecycle.UMOLifecycleAdapterFactory
 * @see org.mule.impl.DefaultLifecycleAdapter
 * @see org.mule.impl.DefaultLifecycleAdapterFactory
 */
public class DefaultLifecycleAdapterFactory implements UMOLifecycleAdapterFactory
{
    public DefaultLifecycleAdapterFactory()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.lifecycle.UMOLifecycleAdapterFactory#create(java.lang.Object,
     *      org.mule.umo.UMODescriptor)
     */
    public UMOLifecycleAdapter create(Object component,
                                      UMODescriptor descriptor,
                                      UMOEntryPointResolver resolver) throws UMOException
    {
        return new DefaultLifecycleAdapter(component, descriptor, resolver);
    }

}
