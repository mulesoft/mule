/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
 * @since 4.5.0
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
