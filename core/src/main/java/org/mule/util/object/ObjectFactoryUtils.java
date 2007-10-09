/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

import org.mule.util.ClassUtils;

public class ObjectFactoryUtils
{

    public static Object createIfNecessary(Object source, Class clazz) throws Exception
    {
        Object result;
        if (source instanceof ObjectFactory)
        {
            result = ((ObjectFactory) source).getOrCreate();
        }
        else
        {
            result = source;
        }
        if (null == result || !(clazz.isAssignableFrom(result.getClass())))
        {
            throw new IllegalArgumentException("Unexpected instance or factory: " + source
                    + " (expected " + ClassUtils.getSimpleName(clazz) + ")");
        }
        return result;
    }

}
