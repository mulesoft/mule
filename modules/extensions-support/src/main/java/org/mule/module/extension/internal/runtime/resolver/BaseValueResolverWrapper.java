/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime.resolver;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Lifecycle;

/**
 * Base class for a {@link ValueResolver} which is a wrapper of another one.
 * This wrapper implements {@link MuleContextAware} and {@link Lifecycle}
 * and propagates those events to the {@link #delegate} if it implements any of the
 * mentioned interfaces.
 *
 * @since 3.7.0
 */
abstract class BaseValueResolverWrapper<T> implements ValueResolver<T>
{
    protected ValueResolver<T> delegate;
    protected MuleContext muleContext;

    BaseValueResolverWrapper(ValueResolver<T> delegate)
    {
        this.delegate = delegate;
    }

    /**
     * Resolves delegating into {@link #delegate#isDynamic()}
     *
     * @return whether the {@link #delegate} is dynamic or not
     */
    @Override
    public boolean isDynamic()
    {
        return delegate.isDynamic();
    }
}
