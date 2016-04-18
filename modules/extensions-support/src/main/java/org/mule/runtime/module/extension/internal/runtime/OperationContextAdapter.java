/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.runtime.Interceptor;
import org.mule.runtime.extension.api.runtime.OperationContext;
import org.mule.runtime.extension.api.runtime.OperationExecutor;

/**
 * Adapter interface which expands the contract of {@link OperationContext} which functionality that is
 * internal to this implementation of the extensions API and shouldn't be accessible for the extensions
 * themselves.
 * <p/>
 * Among other things, it adds the concept of variables, which are key-value pairs in order to contain
 * state that is not specific to the operation but to the extensions framework itself. It's not to contain
 * operation parameters as its {@link #getParameter(String)} counter part. It's meant for things like
 * connection pointers, state to be shared between {@link Interceptor interceptors} and
 * {@link OperationExecutor operation executors}, etc.
 *
 * @since 3.7.0
 */
public interface OperationContextAdapter extends OperationContext
{

    /**
     * Returns the {@link MuleEvent} on which
     * an operation is to be executed
     */
    MuleEvent getEvent();

    /**
     * Returns the value associated with the {@code key}
     *
     * @param key the variable's key
     * @param <T> the generic type for the value
     * @return the value associated with {@code key} or {@code null} if no such variable was registered.
     */
    <T> T getVariable(String key);

    /**
     * Sets a variable of the given {@code key} and {@code value}.
     *
     * @param key   the variable's key. Cannot be {@code null}
     * @param value the associated value. Cannot be {@code null}
     * @return the value previously associated with the {@code key} or {@code null} if no such association existed.
     */
    Object setVariable(String key, Object value);

    /**
     * Removes the variable value associated with {@code key}.
     *
     * @param key the variable's key. Cannot be {@code null}
     * @param <T> the generic type for the removed value
     * @return the value that was associated with the {@code key} or {@code null} if no such association existed
     */
    <T> T removeVariable(String key);

}
