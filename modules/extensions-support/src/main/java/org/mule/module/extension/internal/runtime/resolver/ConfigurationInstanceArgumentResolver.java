/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;

/**
 * An implementation of {@link ArgumentResolver} which
 * returns the value obtained through {@link OperationContextAdapter#getConfigurationInstance()}
 * <p/>
 * Notice that for this to work, the {@link OperationContext}
 * has to be an instance of {@link OperationContextAdapter}
 * <p/>
 * Because this {@link ArgumentResolver} is stateless and thread-safe,
 * it is exposed as a singleton
 *
 * @since 3.7.1
 */
public final class ConfigurationInstanceArgumentResolver implements ArgumentResolver<Object>
{

    private static final ConfigurationInstanceArgumentResolver INSTANCE = new ConfigurationInstanceArgumentResolver();

    public static ConfigurationInstanceArgumentResolver getInstance()
    {
        return INSTANCE;
    }

    private ConfigurationInstanceArgumentResolver()
    {
    }

    @Override
    public Object resolve(OperationContext operationContext)
    {
        return ((OperationContextAdapter) operationContext).getConfigurationInstance();
    }
}
