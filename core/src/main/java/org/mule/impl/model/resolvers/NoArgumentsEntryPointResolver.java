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

import org.mule.umo.UMOEventContext;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.ClassUtils;

/**
 * Allows for arguments with no parameters to be called. Regardless of the payload of the current
 * event, this resolver will always only look for No-Arg servic methods.
 * <p/>
 * Note that the {@link org.mule.impl.model.resolvers.ReflectionEntryPointResolver} supports the resolution
 * of no-arg service methods if the event payload received is of type {@link org.mule.providers.NullPayload}.
 *
 * @see org.mule.impl.model.resolvers.ReflectionEntryPointResolver
 * @see org.mule.providers.NullPayload
 */
public class NoArgumentsEntryPointResolver extends AbstractArgumentEntryPointResolver
{
    //@java.lang.Override
    protected Class[] getMethodArgumentTypes(Object[] payload)
    {
        return ClassUtils.NO_ARGS_TYPE;
    }

    //@java.lang.Override
    protected Object[] getPayloadFromMessage(UMOEventContext context) throws TransformerException
    {
        return ClassUtils.NO_ARGS;
    }
}
