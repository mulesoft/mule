/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
