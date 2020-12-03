package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import static org.mule.runtime.module.extension.internal.runtime.source.legacy.LegacyPollItemStatusUtils.from;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.runtime.source.PollContext;

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
    return from(delegate.accept(consumer));
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
}
