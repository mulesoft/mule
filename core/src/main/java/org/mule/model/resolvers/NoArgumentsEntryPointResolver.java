/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.model.resolvers;

import org.mule.api.MuleEventContext;
import org.mule.api.transformer.TransformerException;
import org.mule.util.ClassUtils;

/**
 * Allows for arguments with no parameters to be called. Regardless of the payload of the current
 * event, this resolver will always only look for No-Arg servic methods.
 * <p/>
 * Note that the {@link org.mule.model.resolvers.ReflectionEntryPointResolver} supports the resolution
 * of no-arg service methods if the event payload received is of type {@link org.mule.transport.NullPayload}.
 *
 * @see org.mule.model.resolvers.ReflectionEntryPointResolver
 * @see org.mule.transport.NullPayload
 */
public class NoArgumentsEntryPointResolver extends AbstractArgumentEntryPointResolver
{
    @Override
    protected Class<?>[] getMethodArgumentTypes(Object[] payload)
    {
        return ClassUtils.NO_ARGS_TYPE;
    }

    @Override
    protected Object[] getPayloadFromMessage(MuleEventContext context) throws TransformerException
    {
        return ClassUtils.NO_ARGS;
    }
}
