/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.interception;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Optional;

/**
 * Manages the interception of {@link org.mule.runtime.core.api.component.Interceptable} processors.
 * <p/>
 * Allows to register {@link ProcessorInterceptorCallback} for {@link ComponentIdentifier componentIdentifiers}.
 * If an {@link ProcessorInterceptorCallback} has been registered for a {@link ComponentIdentifier} then the
 * {@link org.mule.runtime.core.api.processor.Processor#process(Event)} method by using an {@link ProcessorInterceptorCallback} to handle the process behaviour.
 *
 * @since 4.0
 */
public interface ProcessorInterceptionManager {

  /**
   * Registers an {@link ProcessorInterceptorCallback} for a given {@link ComponentIdentifier}.
   *
   * @param componentIdentifier {@link ComponentIdentifier} defines the component to be intercepted. Non null.
   * @param processorInterceptorCallback {@link ProcessorInterceptorCallback} to intercept the {@link org.mule.runtime.core.api.processor.Processor}. Non null.
   * @throws IllegalStateException if there is already a {@link ProcessorInterceptorCallback} registered for given {@link ComponentIdentifier}.
   */
  void registerInterceptionCallback(ComponentIdentifier componentIdentifier,
                                    ProcessorInterceptorCallback processorInterceptorCallback);

  /**
   * Retrieves a {@link Optional<ProcessorInterceptorCallback> callback} that would handle the interception for the processor identified by the componentIdentifier and parameters.
   * In case if the component has not been registered to be intercepted it would return an {@link Optional#empty() empty optinal}.
   *
   * @param componentIdentifier processor identified by the {@link ComponentIdentifier}. Non null.
   * @return {@link Optional<ProcessorInterceptorCallback>} to handle the interception of the processor.
   */
  Optional<ProcessorInterceptorCallback> retrieveInterceptorCallback(ComponentIdentifier componentIdentifier);

}
