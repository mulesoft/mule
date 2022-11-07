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
 * A {@link InternalDistributedTraceContextManager} that retrieves the span name.
 *
 * @since 4.5.0
 */
public class SpanNameInternalDistributedTraceContextVisitor
    implements InternalDistributedTraceContextVisitor<String> {

  @Override
  public String accept(PropagateAllDistributedTraceContextManager manager) {
    return null;
  }

  @Override
  public String accept(SourceDistributedSourceTraceContext manager) {
    return manager.getSpanName();
  }
}
