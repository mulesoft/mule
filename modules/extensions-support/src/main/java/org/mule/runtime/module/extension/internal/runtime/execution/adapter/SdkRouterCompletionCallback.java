/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.adapter;

import static org.mule.runtime.module.extension.internal.runtime.source.legacy.LegacyResultAdapter.from;

import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.process.RouterCompletionCallback;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback} into a
 * {@link RouterCompletionCallback}
 *
 * @since 4.5.0
 */
public class SdkRouterCompletionCallback implements RouterCompletionCallback {

  private org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback delegate;

  public SdkRouterCompletionCallback(org.mule.runtime.extension.api.runtime.process.RouterCompletionCallback delegate) {
    this.delegate = delegate;
  }

  @Override
  public void success(Result<Object, Object> result) {
    delegate.success(from(result));
  }

  @Override
  public void error(Throwable throwable) {
    delegate.error(throwable);
  }
}
