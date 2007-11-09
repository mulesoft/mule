/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.seda.optimised;

import org.mule.impl.model.MuleProxy;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.umo.UMOException;
import org.mule.umo.lifecycle.Callable;

/**
 * Same as <code>SedaComponent</code> except that it assumes that components implement the Callable 
 * interface and therefore does away with the reflection and introspection on objects.
 */
public class OptimisedSedaComponent extends SedaComponent
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 4710126404530397113L;

    /**
     * Default constructor
     */
    public OptimisedSedaComponent()
    {
        super();
    }

    //@Override
    protected MuleProxy createComponentProxy(Object pojoService) throws UMOException
    {
        if (!(pojoService instanceof Callable))
        {
            throw new IllegalArgumentException("Components for the Optimised Mule proxy must implement: "
                                               + Callable.class.getName());
        }
        MuleProxy proxy = new OptimisedMuleProxy((Callable) pojoService, this);
        proxy.setStatistics(getStatistics());
        return proxy;
    }
}
