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
import org.mule.api.construct.FlowConstruct;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.EntryPointResolverSet;

/**
 * <code>NullLifecycleAdapter</code> is a lifecycle adaptor implementation that
 * performs no Mule lifecycle propagation to Mule service component implementations.
 * This can be used when the service component implementation is looked up from a
 * container and therefore has it's own lifecycle management.
 */
public class NullLifecycleAdapter extends DefaultComponentLifecycleAdapter
{

    public NullLifecycleAdapter(Object componentObject,
                                JavaComponent component,
                                FlowConstruct flowConstruct,
                                EntryPointResolverSet entryPointResolver,
                                MuleContext muleContext) throws MuleException
    {
        super(componentObject, component, flowConstruct, entryPointResolver, muleContext);
    }

    @Override
    public void start() throws MuleException
    {
        // no-op
    }

    @Override
    public void stop() throws MuleException
    {
        // no-op
    }

    @Override
    public void dispose()
    {
        // no-op
    }

    @Override
    public boolean isStarted()
    {
        return true;
    }

    @Override
    public boolean isDisposed()
    {
        return false;
    }

    @Override
    public void initialise() throws InitialisationException
    {
        // no-op
    }

}
