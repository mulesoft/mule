/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import org.mule.extension.runtime.OperationContext;
import org.mule.module.extension.internal.runtime.OperationContextAdapter;

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
