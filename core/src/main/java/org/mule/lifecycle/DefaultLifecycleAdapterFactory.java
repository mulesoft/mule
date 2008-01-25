/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.lifecycle;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.LifecycleAdapter;
import org.mule.api.lifecycle.LifecycleAdapterFactory;
import org.mule.api.model.EntryPointResolverSet;
import org.mule.api.service.Service;

/**
 * <code>DefaultLifecycleAdapterFactory</code> creates a DefaultLifeCycleAdapter.  Users can
 * implement their own LifeCycleAdapter factories to control lifecycle events on their services such
 * as introduce other lifecycle events that are controlled by external changes.
 *
 * @see org.mule.api.lifecycle.LifecycleAdapter
 * @see org.mule.api.lifecycle.LifecycleAdapterFactory
 * @see org.mule.lifecycle.DefaultLifecycleAdapter
 * @see org.mule.lifecycle.DefaultLifecycleAdapterFactory
 */
public class DefaultLifecycleAdapterFactory implements LifecycleAdapterFactory
{
    public DefaultLifecycleAdapterFactory()
    {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.mule.api.lifecycle.LifecycleAdapterFactory#create(java.lang.Object,
     *      org.mule.api.UMODescriptor)
     */
    public LifecycleAdapter create(Object pojoService,
                                      Service service,
                                      EntryPointResolverSet resolver) throws MuleException
    {
        return new DefaultLifecycleAdapter(pojoService, service, resolver);
    }

}
