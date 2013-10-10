/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.component;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.JavaComponent;
import org.mule.api.component.LifecycleAdapter;
import org.mule.api.component.LifecycleAdapterFactory;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.model.EntryPointResolverSet;

/**
 * <code>DefaultComponentLifecycleAdapterFactory</code> creates a DefaultComponentLifecycleAdapter.  Users can
 * implement their own LifeCycleAdapter factories to control lifecycle events on their services such
 * as introduce other lifecycle events that are controlled by external changes.
 *
 * @see org.mule.api.component.LifecycleAdapter
 * @see org.mule.api.component.LifecycleAdapterFactory
 * @see org.mule.component.DefaultComponentLifecycleAdapter
 * @see org.mule.component.DefaultComponentLifecycleAdapterFactory
 */
public class DefaultComponentLifecycleAdapterFactory implements LifecycleAdapterFactory
{

    public LifecycleAdapter create(Object pojoService,
                                   JavaComponent component,
                                   FlowConstruct flowConstruct,
                                   EntryPointResolverSet resolver,
                                   MuleContext muleContext) throws MuleException
    {
        return new DefaultComponentLifecycleAdapter(pojoService, component, flowConstruct, resolver, muleContext);
    }

}
