/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.processor;

/**
 * Adds clone capability to {@link Processor}
 * <p/>
 * NOTE: Lifecycle management is shared with the original {@link Processor} instance
 */
public interface CloneableMessageProcessor {

  /**
   * Creates a new instance cloned from the current one
   *
   * @return a not null {@link Processor}
   */
  Processor clone();
}
