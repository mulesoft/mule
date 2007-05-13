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

import org.mule.impl.MuleDescriptor;
import org.mule.impl.model.seda.SedaComponent;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.util.ObjectPool;

/**
 * A Seda component runs inside a Seda Model and is responsible for managing a Seda
 * Queue and thread pool for a Mule sevice component. In Seda terms this is
 * equivalent to a stage.
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
    public OptimisedSedaComponent(MuleDescriptor descriptor, OptimisedSedaModel model)
    {
        super(descriptor, model);
    }

    // @Override
    protected ObjectPool createPool() throws InitialisationException
    {
        return this.getPoolingProfile().getPoolFactory().createPool(descriptor, model,
            new OptimisedProxyFactory(descriptor, model), getPoolingProfile());
    }
}
