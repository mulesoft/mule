/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model.resolvers;

import org.mule.umo.UMODescriptor;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.model.UMOEntryPointResolver;

/**
 * An entrypoint resolver that only allows Service objects that implmement the
 * Callable interface
 * 
 * @see org.mule.umo.lifecycle.Callable
 */
public class CallableEntryPointResolver implements UMOEntryPointResolver
{
    public UMOEntryPoint resolveEntryPoint(UMODescriptor componentDescriptor) throws ModelException
    {
        return new CallableEntryPoint();
    }
}
