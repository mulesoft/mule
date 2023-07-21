/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.util.function.Supplier;

/**
 * Wraps different interceptor factories so they can be used seamlessly from {@link ReactiveInterceptionAction} and
 * {@link ReactiveAroundInterceptorAdapter}.
 *
 * @since 4.4
 */
interface ComponentInterceptorFactoryAdapter extends Supplier<ComponentInterceptorAdapter> {

  /**
   * Determines if an {@link ComponentInterceptorAdapter} could be created by this factory to be applied to a component based on
   * some of its attributes.
   *
   * @param component the location and identification properties of the to-be intercepted component in the mule app configuration.
   * @return {@code true} if this handler could be applied to the component with the provided parameters, {@code false} otherwise.
   */
  boolean isInterceptable(ReactiveProcessor component);

  /**
   * Determines if an {@link ComponentInterceptorAdapter} shall be created by this factory to be applied to a component based on
   * some of its attributes.
   *
   * @param location the location and identification properties of the to-be intercepted component in the mule app configuration.
   * @return {@code true} if this handler must be applied to the component with the provided parameters, {@code false} otherwise.
   */
  boolean intercept(ComponentLocation componentLocation);

}
