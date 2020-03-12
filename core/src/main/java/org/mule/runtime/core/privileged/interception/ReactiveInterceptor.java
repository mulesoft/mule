/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.interception;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.function.BiFunction;

/**
 * Adds behavior to the {@link ReactiveProcessor} for a {@link Processor}.
 * <p>
 * Implementations receive the {@link Processor} definition and the currently enhanced {@link ReactiveProcessor}, and return a new
 * {@link ReactiveProcessor} that wraps the received one with any additional functionality.
 *
 * @since 4.3
 */
@NoImplement
public interface ReactiveInterceptor extends BiFunction<Processor, ReactiveProcessor, ReactiveProcessor> {

}
