/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.PollContext;
import org.mule.sdk.api.runtime.source.PollingSource;
import org.mule.sdk.api.runtime.source.Source;
import org.mule.sdk.api.runtime.source.SourceCallback;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import org.slf4j.Logger;

/**
 * Adapts a legacy {@link org.mule.runtime.extension.api.runtime.source.Source} into a {@link Source}
 *
 * @param <T> the generic type of the output value
 * @param <A> the generic type of the message attributes
 * @since 4.4.0
 */
public class SdkSourceAdapter<T, A> extends Source<T, A> implements LegacySourceWrapper, Initialisable, Disposable {

  private static final Logger LOGGER = getLogger(SdkSourceAdapter.class);

  private final org.mule.runtime.extension.api.runtime.source.Source<T, A> delegate;

  public static <T, A> Source<T, A> from(org.mule.runtime.extension.api.runtime.source.Source<T, A> delegate) {
    if (delegate instanceof org.mule.runtime.extension.api.runtime.source.PollingSource) {
      return new LegacyPollingSourceAdapter<>((org.mule.runtime.extension.api.runtime.source.PollingSource) delegate);
    }

    return new SdkSourceAdapter<>(delegate);
  }

  private SdkSourceAdapter(org.mule.runtime.extension.api.runtime.source.Source<T, A> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void onStart(SourceCallback<T, A> sourceCallback) throws MuleException {
    delegate.onStart(new LegacySourceCallbackAdapter(sourceCallback));
  }

  @Override
  public void onStop() {
    delegate.onStop();
  }

  @Override
  public org.mule.runtime.extension.api.runtime.source.Source getDelegate() {
    return delegate;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(delegate);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(delegate, LOGGER);
  }

  private static class LegacyPollingSourceAdapter<T, A> extends PollingSource<T, A>
      implements LegacySourceWrapper, Initialisable, Disposable {

    private static final Logger LOGGER = getLogger(LegacyPollingSourceAdapter.class);

    private final org.mule.runtime.extension.api.runtime.source.PollingSource<T, A> delegate;

    private LegacyPollingSourceAdapter(org.mule.runtime.extension.api.runtime.source.PollingSource<T, A> delegate) {
      this.delegate = delegate;
    }

    @Override
    protected void doStart() throws MuleException {
      delegate.onStart(null);
    }

    @Override
    protected void doStop() {
      delegate.onStop();
    }

    @Override
    public void poll(PollContext<T, A> pollContext) {
      delegate.poll(new LegacyPollContextAdapter<>(pollContext));
    }

    @Override
    public void onRejectedItem(Result<T, A> result, SourceCallbackContext callbackContext) {
      delegate.onRejectedItem(LegacyResultAdapter.from(result), new LegacySourceCallbackContextAdapterAdapter(callbackContext));
    }

    @Override
    public org.mule.runtime.extension.api.runtime.source.Source getDelegate() {
      return delegate;
    }

    @Override
    public void initialise() throws InitialisationException {
      initialiseIfNeeded(delegate);
    }

    @Override
    public void dispose() {
      disposeIfNeeded(delegate, LOGGER);
    }
  }
}
