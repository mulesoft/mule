/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.runtime.source.SourceCallback} into a {@link SourceCallback}
 *
 * @param <T> the generic type of the output value
 * @param <A> the generic type of the message attributes
 * @since 4.4.0
 */
public class SdkSourceCallbackAdapter<T, A> implements SourceCallback<T, A> {

  private final org.mule.runtime.extension.api.runtime.source.SourceCallback<T, A> delegate;

  public SdkSourceCallbackAdapter(org.mule.runtime.extension.api.runtime.source.SourceCallback<T, A> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(Result<T, A> result) {
    delegate.handle(LegacyResultAdapter.from(result));
  }

  @Override
  public void handle(Result<T, A> result, SourceCallbackContext context) {
    delegate.handle(LegacyResultAdapter.from(result), new LegacySourceCallbackContextAdapter(context));
  }

  @Override
  public void onConnectionException(ConnectionException e) {
    delegate.onConnectionException(e);
  }

  @Override
  public SourceCallbackContext createContext() {
    return new SdkSourceCallBackContextAdapter(delegate.createContext());
  }
}
