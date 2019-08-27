/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

public class FluxSinkRecorderExecutorCallback implements ExecutorCallback {

  private final FluxSinkRecorder<CoreEvent> sinkRecorder;

  public FluxSinkRecorderExecutorCallback(FluxSinkRecorder<CoreEvent> sinkRecorder) {
    this.sinkRecorder = sinkRecorder;
  }

  @Override
  public void complete(Object value) {
    sinkRecorder.next((CoreEvent) value);
  }

  @Override
  public void error(Throwable e) {
    sinkRecorder.error(e);
  }
}
