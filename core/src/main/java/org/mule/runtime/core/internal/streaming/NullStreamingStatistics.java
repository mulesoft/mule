/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming;

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
  public int getOpenCursorProvidersCount() {
    return 0;
  }

  @Override
  public int getOpenCursorsCount() {
    return 0;
  }
}
