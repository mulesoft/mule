/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.registry;

import org.mule.api.MuleContext;
import org.mule.api.registry.ObjectLimbo;

/**
 * Default implementation of {@link ObjectLimbo}. This implementation is thread-safe
 *
 * @since 3.7.0
 */
public class DefaultObjectLimbo extends SimpleRegistry implements ObjectLimbo
{

    public DefaultObjectLimbo(MuleContext muleContext)
    {
        super(muleContext);
        // SimpleRegistry always adds some stuff by default
        clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        getRegistryMap().clear();
    }
}
