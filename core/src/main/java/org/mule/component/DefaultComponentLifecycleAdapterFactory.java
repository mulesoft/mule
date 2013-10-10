/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
