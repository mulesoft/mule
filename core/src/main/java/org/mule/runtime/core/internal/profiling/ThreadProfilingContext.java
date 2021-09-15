/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling;

import static java.util.Objects.requireNonNull;

import java.util.Optional;

public class ThreadProfilingContext {

  private static final ThreadLocal<ThreadProfilingContext> currentThreadProfilingContext =
      ThreadLocal.withInitial(ThreadProfilingContext::new);

  private OperationMetadata runningOperationMetadata;

  private ThreadProfilingContext() {}

  public static ThreadProfilingContext currentThreadProfilingContext() {
    return currentThreadProfilingContext.get();
  }

  public ThreadProfilingContext replaceWith(ThreadProfilingContext newContext) {
    requireNonNull(newContext);
    ThreadProfilingContext currentContext = ThreadProfilingContext.currentThreadProfilingContext.get();
    currentContext.setRunningOperationMetadata(newContext.getRunningOperationMetadata().orElse(null));
    return currentContext;
  }

  public Optional<OperationMetadata> getRunningOperationMetadata() {
    return Optional.ofNullable(runningOperationMetadata);
  }

  // TODO: This will be called by the PS at the same point than the STARTING_OPERATION_EXECUTION profiling event emission
  public ThreadProfilingContext setRunningOperationMetadata(OperationMetadata runningOperationMetadata) {
    requireNonNull(runningOperationMetadata);
    this.runningOperationMetadata = runningOperationMetadata;
    return this;
  }

  // TODO: This will be called by the execution engine when the operation has returned the control to the runtime (callback)
  public ThreadProfilingContext clearRunningOperationMetadata() {
    this.runningOperationMetadata = null;
    return this;
  }
}
