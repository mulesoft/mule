/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * Specialization of {@link ReactiveProcessor} that allows performing certain optimizations when the processor is not blocking, as
 * indicated by its {@link #isBlocking()} method.
 * </p>
 * <b>IMPORTANT!</b> The processing strategy will delegate the parallel processing of events to the implementation if it is
 * not blocking, so it is required that implementations properly handle parallel processing in this case.
 *
 * @since 4.3
 */
public interface ComponentInnerProcessor extends ReactiveProcessor {

  /**
   * Indicates that the component is blocking.
   *
   * @return {@code} if the processor is blocking.
   */
  boolean isBlocking();

}
