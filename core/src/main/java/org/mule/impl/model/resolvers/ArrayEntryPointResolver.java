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
 * Will resolver entry point methods on a service component that accept a single array.
 * i.e.
 * <code>public Object eat(Fruit[] fruit)</code>
 * <p/>
 * This resolver will NOT resolver method endtry points such as -
 * <code>public Object eat(Fruit[] fruit, Banana banana)</code>
 * <p/>
 * If you require to mix an array type with complex types you need to specify an inbound transformer that return a
 * multi-dimensional array of arguments i.e.
 * <code>new Object[]{new Fruit[]{new Apple(), new Orange()}, new Banana()};</code>
 */
public class ArrayEntryPointResolver extends AbstractArgumentEntryPointResolver
{
    //@java.lang.Override
    protected Class[] getMethodArgumentTypes(Object[] payload)
    {
        return ClassUtils.getClassTypes(payload);
    }

    //@java.lang.Override
    protected Object[] getPayloadFromMessage(UMOEventContext context) throws TransformerException
    {
        Object temp;
        if (isTransformFirst())
        {
            temp = context.getTransformedMessage();
        }
        else
        {
            temp = context.getMessage().getPayload();
        }
        if (temp instanceof Object[])
        {
            return new Object[]{temp};
        }
        else
        {
            //Payload type not supported by this resolver
            return null;
        }
    }
}
