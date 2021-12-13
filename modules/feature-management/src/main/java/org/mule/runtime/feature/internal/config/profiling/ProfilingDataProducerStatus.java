/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.config.profiling;

/**
 * The status for a {@link org.mule.runtime.api.profiling.ProfilingDataProducer}.
 */
public interface ProfilingDataProducerStatus {

  /**
   * @return whether it is enabled or disabled.
   */
  boolean isEnabled();

  /**
   * resets the status. Useful for propagating changes if needed.
   */
  void reset();
}
