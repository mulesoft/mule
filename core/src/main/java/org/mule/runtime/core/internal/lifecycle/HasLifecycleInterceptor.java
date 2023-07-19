/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.lifecycle;

/**
 * Contract interface for an object on which a {@link LifecycleInterceptor} can be set into
 *
 * @since 3.8
 */
public interface HasLifecycleInterceptor {

  /**
   * Sets the given {@code interceptor}
   *
   * @param interceptor a {@link LifecycleInterceptor}
   */
  void setLifecycleInterceptor(LifecycleInterceptor interceptor);
}
