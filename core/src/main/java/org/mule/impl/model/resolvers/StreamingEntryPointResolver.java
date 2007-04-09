/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.model.resolvers;

import org.mule.umo.UMODescriptor;
import org.mule.umo.model.ModelException;
import org.mule.umo.model.UMOEntryPoint;
import org.mule.umo.model.UMOEntryPointResolver;

/**
 * Creates a {@link org.mule.impl.model.resolvers.StreamingEntryPoint}. For use with the Streaming Model.
 * @see org.mule.impl.model.streaming.StreamingModel
 */
public class StreamingEntryPointResolver implements UMOEntryPointResolver
{
    public UMOEntryPoint resolveEntryPoint(UMODescriptor componentDescriptor) throws ModelException
    {
        return new StreamingEntryPoint();
    }
}
