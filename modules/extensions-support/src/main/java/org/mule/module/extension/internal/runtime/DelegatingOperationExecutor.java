/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.runtime;

import org.mule.extension.runtime.OperationExecutor;

/**
 * An adapter interface for an {@link OperationExecutor} which
 * delegates execution into a custom object of type {@code T}.
 *
 * @param <T> the type of the delegate object
 * @since 3.7.0
 */
public interface DelegatingOperationExecutor<T> extends OperationExecutor
{

    /**
     * @return the object in which this delegate ultimately delegates into
     */
    T getExecutorDelegate();
}
