/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.util.List;
import java.util.Optional;
import java.util.function.LongConsumer;

class PayloadStatisticsPagingProvider<C, T> implements PagingProvider<C, T> {

  private final PagingProvider<C, T> decorated;
  private final LongConsumer populator;

  public PayloadStatisticsPagingProvider(PagingProvider<C, T> decorated, LongConsumer populator) {
    this.decorated = decorated;
    this.populator = populator;
  }

  @Override
  public List<T> getPage(C connection) {
    final List<T> page = decorated.getPage(connection);
    populator.accept(page.size());
    return page;
  }

  @Override
  public Optional<Integer> getTotalResults(C connection) {
    return decorated.getTotalResults(connection);
  }

  @Override
  public void close(C connection) throws MuleException {
    decorated.close(connection);
  }

}
