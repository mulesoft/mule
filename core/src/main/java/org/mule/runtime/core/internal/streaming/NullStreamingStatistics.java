/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.streaming;

/**
 * Null implementation of {@link MutableStreamingStatistics}. All methods simply return zero.
 *
 * @since 4.2.0
 */
public class NullStreamingStatistics implements MutableStreamingStatistics {

  @Override
  public int incrementOpenProviders() {
    return 0;
  }

  @Override
  public int decrementOpenProviders() {
    return 0;
  }

  @Override
  public int incrementOpenCursors() {
    return 0;
  }

  @Override
  public int decrementOpenCursors() {
    return 0;
  }

  @Override
  public int decrementOpenCursors(int howMany) {
    return 0;
  }

  @Override
  public int getOpenCursorProvidersCount() {
    return 0;
  }

  @Override
  public int getOpenCursorsCount() {
    return 0;
  }
}
