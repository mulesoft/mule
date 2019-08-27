/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.internal.util.rx.FluxSinkSupplier;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;

public class FluxSinkSupplierExecutorCallback<T> implements CompletableComponentExecutor.ExecutorCallback {

  private final FluxSinkSupplier<T> sinkSupplier;

  public FluxSinkSupplierExecutorCallback(FluxSinkSupplier<T> sinkSupplier) {
    this.sinkSupplier = sinkSupplier;
  }

  @Override
  public void complete(Object value) {
    sinkSupplier.get().next((T) value);
  }

  @Override
  public void error(Throwable e) {
    sinkSupplier.get().error(e);
  }
}
