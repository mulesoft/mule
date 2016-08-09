/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.api;

import static java.lang.Thread.*;
import static org.springframework.util.ClassUtils.forName;

import org.mule.runtime.core.api.MuleRuntimeException;

import org.springframework.util.ClassUtils;

/**
 * Set of common {@link TypeConverter}s to be reused in different
 * {@link ComponentBuildingDefinitionProvider}s
 *
 * @since 4.0
 */
public class CommonTypeConverters
{

    /**
     * @return a converter that transforms class name to a {@code Class} instance.
     */
    public static TypeConverter<String, Class> stringToClassConverter()
    {
        return className -> {
            try
            {
                return forName(className, currentThread().getContextClassLoader());
            }
            catch (ClassNotFoundException e)
            {
                throw new MuleRuntimeException(e);
            }
        };
    }

}
