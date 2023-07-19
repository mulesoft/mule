/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
