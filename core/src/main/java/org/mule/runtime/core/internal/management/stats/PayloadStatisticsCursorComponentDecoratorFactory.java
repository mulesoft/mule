/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static org.mule.runtime.core.internal.management.stats.NoOpCursorComponentDecoratorFactory.NO_OP_INSTANCE;

import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.runtime.extension.api.runtime.streaming.PagingProvider;

import java.io.InputStream;
import java.util.Iterator;

class PayloadStatisticsCursorComponentDecoratorFactory implements CursorComponentDecoratorFactory {

  private final PayloadStatistics payloadStatistics;

  public PayloadStatisticsCursorComponentDecoratorFactory(PayloadStatistics payloadStatistics) {
    this.payloadStatistics = payloadStatistics;
  }

  @Override
  public <T> Iterator<T> decorateInput(Iterator<T> decorated, String correlationId) {
    if (payloadStatistics.isEnabled()) {
      return new PayloadStatisticsIterator(decorated, payloadStatistics::addInputObjectCount);
    } else {
      return NO_OP_INSTANCE.decorateInput(decorated, correlationId);
    }
  }

  @Override
  public InputStream decorateInput(InputStream decorated, String correlationId) {
    if (payloadStatistics.isEnabled()) {
      return new PayloadStatisticsInputStream(decorated, payloadStatistics::addInputByteCount);
    } else {
      return NO_OP_INSTANCE.decorateInput(decorated, correlationId);
    }
  }

  @Override
  public <C, T> PagingProvider<C, T> decorateOutput(PagingProvider<C, T> decorated, String correlationId) {
    if (payloadStatistics.isEnabled()) {
      return new PayloadStatisticsPagingProvider<>(decorated, payloadStatistics::addOutputObjectCount);
    } else {
      return NO_OP_INSTANCE.decorateOutput(decorated, correlationId);
    }
  }

  @Override
  public <T> Iterator<T> decorateOutput(Iterator<T> decorated, String correlationId) {
    if (payloadStatistics.isEnabled()) {
      return new PayloadStatisticsIterator(decorated, payloadStatistics::addOutputObjectCount);
    } else {
      return NO_OP_INSTANCE.decorateOutput(decorated, correlationId);
    }
  }

  @Override
  public InputStream decorateOutput(InputStream decorated, String correlationId) {
    if (payloadStatistics.isEnabled()) {
      return new PayloadStatisticsInputStream(decorated, payloadStatistics::addOutputByteCount);
    } else {
      return NO_OP_INSTANCE.decorateOutput(decorated, correlationId);
    }
  }
}
