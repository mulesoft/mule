/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transport;

/**
 * Implements {@link org.mule.runtime.core.transport.ConfigurableKeyedObjectPoolFactory} creating instances
 * of {@link DefaultConfigurableKeyedObjectPool}.
 */
public class DefaultConfigurableKeyedObjectPoolFactory implements ConfigurableKeyedObjectPoolFactory
{

    public ConfigurableKeyedObjectPool createObjectPool()
    {
        return new DefaultConfigurableKeyedObjectPool();
    }
}
