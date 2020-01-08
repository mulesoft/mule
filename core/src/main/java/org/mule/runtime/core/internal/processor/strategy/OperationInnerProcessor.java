/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * Specialization of {@link ReactiveProcessor} that allows to perform certain optimizations when the processor is not
 * asynchronous, as indicated by its {@link #isAsync()} method.
 *
 * @since 4.3
 */
public interface OperationInnerProcessor extends ReactiveProcessor {

  /**
   * An async processor is one that eventually changes the thread where it is executing.
   *
   * @return {@code} if the processor is asynchronous.
   */
  boolean isAsync();
}
