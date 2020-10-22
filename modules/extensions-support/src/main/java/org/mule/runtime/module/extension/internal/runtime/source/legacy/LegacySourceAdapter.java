/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.api.exception.MuleException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.PollContext;
import org.mule.sdk.api.runtime.source.PollingSource;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

public class LegacySourceAdapter<T, A> extends Source<T, A> {

  private final org.mule.runtime.extension.api.runtime.source.Source<T, A> delegate;

  public static <T, A> Source<T, A> from(org.mule.runtime.extension.api.runtime.source.Source<T, A> delegate) {
    if (delegate instanceof org.mule.runtime.extension.api.runtime.source.PollingSource) {
      return new LegacyPollingSourceAdapter<>((org.mule.runtime.extension.api.runtime.source.PollingSource) delegate);
    }

    return new LegacySourceAdapter<>(delegate);
  }

  private LegacySourceAdapter(org.mule.runtime.extension.api.runtime.source.Source<T, A> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void onStart(SourceCallback<T, A> sourceCallback) throws MuleException {
//    delegate.onStart(sourceCallback);

  }

  @Override
  public void onStop() {
    delegate.onStop();
  }

  private static class LegacyPollingSourceAdapter<T, A> extends PollingSource<T, A> {

    private final org.mule.runtime.extension.api.runtime.source.PollingSource<T, A> delegate;

    private LegacyPollingSourceAdapter(org.mule.runtime.extension.api.runtime.source.PollingSource<T, A> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected void doStart() throws MuleException {
      delegate.onStart();
    }

    @Override
    protected void doStop() {
      delegate.onStop();
    }

    @Override
    public void poll(PollContext<T, A> pollContext) {
      delegate.poll(pollContext);
    }

    @Override
    public void onRejectedItem(Result<T, A> result, SourceCallbackContext callbackContext) {
      delegate.onRejectedItem(result, callbackContext);
    }
  }
}
