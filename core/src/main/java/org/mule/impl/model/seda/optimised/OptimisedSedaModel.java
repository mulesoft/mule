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

import org.mule.impl.model.seda.SedaModel;

/**
 * Same as <code>SedaModel</code> except that it assumes that components implement the Callable 
 * interface and therefore does away with the reflection and introspection on objects.
 */
public class OptimisedSedaModel extends SedaModel
{
    /**
     * Returns the model type name. This is a friendly identifier that is used to
     * look up the SPI class for the model
     * 
     * @return the model type
     */
    public String getType()
    {
        return "seda-optimised";
    }
}
