/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.adapter;

import static org.mule.runtime.module.extension.internal.runtime.source.legacy.LegacyResultAdapter.from;

import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.process.CompletionCallback;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.runtime.process.CompletionCallback} into a {@link CompletionCallback}
 *
 * @param <T> The generic type of the operation's output value
 * @param <A> The generic type of the operation's output attributes
 *
 * @since 4.4.0
 */
public class SdkCompletionCallbackAdapter<T, A> implements CompletionCallback<T, A> {

  private final org.mule.runtime.extension.api.runtime.process.CompletionCallback delegate;

  public SdkCompletionCallbackAdapter(org.mule.runtime.extension.api.runtime.process.CompletionCallback delegate) {
    this.delegate = delegate;
  }

  @Override
  public void success(Result<T, A> result) {
    delegate.success(from(result));
  }

  @Override
  public void error(Throwable e) {
    delegate.error(e);
  }
}
