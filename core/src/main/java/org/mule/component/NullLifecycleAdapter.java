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
