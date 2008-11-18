/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.component;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.component.JavaComponent;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.model.EntryPointResolverSet;

/**
 * <code>NullLifecycleAdapter</code> is a lifecycle adaptor implementation that
 * performs no Mule lifecycle propagation to Mule service component implementations.
 * This can be used when the service component implementation is looked up from a
 * container and therefore has it's own lifecycle management.
 */
public class NullLifecycleAdapter extends DefaultLifecycleAdapter
{

    public NullLifecycleAdapter(Object componentObject,
                                JavaComponent component,
                                EntryPointResolverSet entryPointResolver,
                                MuleContext muleContext) throws MuleException
    {
        super(componentObject, component, entryPointResolver, muleContext);
    }

    public void start() throws MuleException
    {
        // no-op
    }

    public void stop() throws MuleException
    {
        // no-op
    }

    public void dispose()
    {
        // no-op
    }

    public boolean isStarted()
    {
        return true;
    }

    public boolean isDisposed()
    {
        return false;
    }

    public void initialise() throws InitialisationException
    {
        // no-op
    }

}
