/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;

import java.util.function.Consumer;

/**
 * Refer to {@link FlowExceptionHandler#router(java.util.function.Function, Consumer, Consumer)}.
 * <p>
 * Also implements {@link Disposable} to allow shutdown of any child objects.
 *
 * @since 4.3
 */
public interface ExceptionRouter extends Consumer<Exception>, Disposable {

}
