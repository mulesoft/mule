/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.interception;

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
public interface ReactiveInterceptor extends BiFunction<ReactiveProcessor, ReactiveProcessor, ReactiveProcessor> {

}
