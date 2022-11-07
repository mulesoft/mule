/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.extension.internal.runtime.tracing;

import org.mule.runtime.module.extension.internal.runtime.parameter.PropagateAllDistributedTraceContextManager;
import org.mule.runtime.module.extension.internal.runtime.source.trace.SourceDistributedSourceTraceContext;

/**
 * A visitor for distributed trace context
 *
 * @param <T> the type to return
 */
public interface InternalDistributedTraceContextVisitor<T> {

  /**
   * @param manager the {@link PropagateAllDistributedTraceContextManager} to accept.
   * @return the result of the visit operation.
   */
  T accept(PropagateAllDistributedTraceContextManager manager);

  /**
   * @param manager the {@link SourceDistributedSourceTraceContext} to accept.
   * @return the result of the visit operation.
   */
  T accept(SourceDistributedSourceTraceContext manager);
}
