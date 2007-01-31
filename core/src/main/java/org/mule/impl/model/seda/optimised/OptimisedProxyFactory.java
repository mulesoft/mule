/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.seda.optimised;

import org.mule.config.pool.CommonsPoolProxyFactory;
import org.mule.impl.MuleDescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOModel;
import org.mule.umo.lifecycle.Callable;

/**
 * Creates an optimised Mule proxy for pooling which does away with the reflection
 * and inteception on objects
 */
public class OptimisedProxyFactory extends CommonsPoolProxyFactory
{
    public OptimisedProxyFactory(MuleDescriptor descriptor, UMOModel model)
    {
        super(descriptor, model);
    }

    protected Object createProxy(Object component) throws UMOException
    {
        if (!(component instanceof Callable))
        {
            throw new IllegalArgumentException("Components for the Optimised Mule proxy must implement: "
                                               + Callable.class.getName());
        }
        return new OptimisedMuleProxy((Callable)component, descriptor, pool);
    }
}
