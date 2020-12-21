/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import static org.mule.runtime.module.extension.internal.runtime.source.legacy.LegacyPollItemStatusUtils.from;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.core.internal.util.message.SdkResultAdapter;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.PollContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;

public class SdkToLegacyPollContextAdapter<T, A> implements PollContext<T, A> {

  private final org.mule.sdk.api.runtime.source.PollContext delegate;

  public SdkToLegacyPollContextAdapter(org.mule.sdk.api.runtime.source.PollContext<T, A> delegate) {
    this.delegate = delegate;
  }

  @Override
  public PollItemStatus accept(Consumer<PollItem<T, A>> consumer) {
    return from(delegate.accept(new PollItemConsumerAdapter(consumer)));
  }

  @Override
  public Optional<Serializable> getWatermark() {
    return delegate.getWatermark();
  }

  @Override
  public boolean isSourceStopping() {
    return delegate.isSourceStopping();
  }

  @Override
  public void setWatermarkComparator(Comparator<? extends Serializable> comparator) {
    delegate.setWatermarkComparator(comparator);
  }

  @Override
  public void onConnectionException(ConnectionException e) {
    delegate.onConnectionException(e);
  }

  private static class PollItemConsumerAdapter<T, A>
      implements Consumer<org.mule.sdk.api.runtime.source.PollContext.PollItem<T, A>> {

    Consumer<PollItem<T, A>> delegate;

    PollItemConsumerAdapter(Consumer<PollItem<T, A>> delegate) {
      this.delegate = delegate;
    }

    @Override
    public void accept(org.mule.sdk.api.runtime.source.PollContext.PollItem pollItem) {
      delegate.accept(new SdkToLegacyPollItemAdapter(pollItem));
    }

    private static class SdkToLegacyPollItemAdapter implements PollContext.PollItem {

      org.mule.sdk.api.runtime.source.PollContext.PollItem delegate;

      public SdkToLegacyPollItemAdapter(org.mule.sdk.api.runtime.source.PollContext.PollItem delegate) {
        this.delegate = delegate;
      }

      @Override
      public SourceCallbackContext getSourceCallbackContext() {
        return new SdkToLegacySourceCallbackContextAdapter(delegate.getSourceCallbackContext());
      }

      @Override
      public PollItem setResult(Result result) {
        delegate.setResult(SdkResultAdapter.from(result));
        return this;
      }

      @Override
      public PollItem setWatermark(Serializable watermark) {
        delegate.setWatermark(watermark);
        return this;
      }

      @Override
      public PollItem setId(String id) {
        delegate.setId(id);
        return this;
      }
    }
  }
}
