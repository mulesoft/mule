/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.execution.tracing;

import org.mule.runtime.core.internal.trace.DistributedTraceContext;

/**
 * A component that carries information about the distributed trace context
 *
 * @since 4.5.0
 */
public interface DistributedTraceContextAware {

  /**
   * @return the {@link DistributedTraceContext} associated with the event.
   *
   * @since 4.5.0
   */
  DistributedTraceContext getDistributedTraceContext();

  /**
   * @param distributedTraceContext the distributed trace context to set.
   *
   * @since 4.5.0
   */
  void setDistributedTraceContext(DistributedTraceContext distributedTraceContext);
}
