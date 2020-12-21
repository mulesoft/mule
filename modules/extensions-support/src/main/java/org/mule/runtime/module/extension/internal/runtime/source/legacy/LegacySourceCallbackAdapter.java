/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.internal.util.message.SdkResultAdapter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.sdk.api.runtime.source.SourceCallback;

/**
 * Adapts a {@link SourceCallback} into a legacy {@link org.mule.runtime.extension.api.runtime.source.SourceCallback}
 *
 * @param <T> the generic type of the output value
 * @param <A> the generic type of the message attributes
 * @since 4.4.0
 */
public class LegacySourceCallbackAdapter<T, A>
    implements org.mule.runtime.extension.api.runtime.source.SourceCallback<T, A> {

  private final SourceCallback<T, A> sourceCallback;

  public LegacySourceCallbackAdapter(SourceCallback<T, A> sourceCallback) {
    this.sourceCallback = sourceCallback;
  }

  @Override
  public void handle(Result<T, A> result) {
    sourceCallback.handle(SdkResultAdapter.from(result));
  }

  @Override
  public void handle(Result<T, A> result, SourceCallbackContext context) {
    sourceCallback.handle(SdkResultAdapter.from(result), new SdkSourceCallBackContextAdapter(context));
  }

  @Override
  public void onConnectionException(ConnectionException e) {
    sourceCallback.onConnectionException(e);
  }

  @Override
  public SourceCallbackContext createContext() {
    return new LegacySourceCallbackContextAdapterAdapter(sourceCallback.createContext());
  }
}
