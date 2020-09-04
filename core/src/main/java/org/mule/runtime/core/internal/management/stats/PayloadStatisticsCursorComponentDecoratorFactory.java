/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.internal.management.stats.NoOpCursorComponentDecoratorFactory.NO_OP_INSTANCE;

import org.mule.runtime.core.api.management.stats.CursorComponentDecoratorFactory;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.streaming.PagingProvider;

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.iterators.AbstractIteratorDecorator;

class PayloadStatisticsCursorComponentDecoratorFactory implements CursorComponentDecoratorFactory {

  private final PayloadStatistics payloadStatistics;

  public PayloadStatisticsCursorComponentDecoratorFactory(PayloadStatistics payloadStatistics) {
    this.payloadStatistics = payloadStatistics;
  }

  @Override
  public void incrementInvocationCount(String correlationId) {
    if (payloadStatistics.isEnabled()) {
      payloadStatistics.incrementInvocationCount();
    }
  }

  @Override
  public <T> Collection<T> decorateInput(Collection<T> decorated, String correlationId) {
    if (payloadStatistics.isEnabled()) {
      if (decorated instanceof List) {
        return new PayloadStatisticsList((List) decorated, payloadStatistics::addInputObjectCount);
      } else if (decorated instanceof Set) {
        return new PayloadStatisticsSet((Set) decorated, payloadStatistics::addInputObjectCount);
      } else {
        return new PayloadStatisticsCollection(decorated, payloadStatistics::addInputObjectCount);
      }
    } else {
      return NO_OP_INSTANCE.decorateInput(decorated, correlationId);
    }
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

  @Override
  public Collection<Result> decorateOutputResultCollection(Collection<Result> decorated,
                                                           String correlationId) {
    if (payloadStatistics.isEnabled()) {
      payloadStatistics.addOutputObjectCount(decorated.size());
      return decorated.stream()
          .map(r -> decorateResult(r, correlationId))
          .collect(toList());
    } else {
      return NO_OP_INSTANCE.decorateOutputResultCollection(decorated, correlationId);
    }
  }

  @Override
  public Iterator<Result> decorateOutputResultIterator(Iterator<Result> decorated, String correlationId) {
    if (payloadStatistics.isEnabled()) {
      return decorateOutput(new AbstractIteratorDecorator(decorated) {

        @Override
        public Object next() {
          return decorateResult((Result) super.next(), correlationId);
        }
      }, correlationId);
    } else {
      return NO_OP_INSTANCE.decorateOutputResultIterator(decorated, correlationId);
    }
  }

  private Result decorateResult(Result decorated, String correlationId) {
    Object decoratedOutput = decorateOutput(decorated.getOutput(), correlationId);

    if (decoratedOutput == decorated.getOutput()) {
      return decorated;
    } else {
      return decorated.copy().output(decoratedOutput).build();
    }
  }

  private Object decorateOutput(Object decorated, String correlationId) {
    if (decorated instanceof InputStream) {
      return decorateOutput((InputStream) decorated, correlationId);
    } else if (decorated instanceof Iterator) {
      return decorateOutput((Iterator) decorated, correlationId);
    } else {
      return decorated;
    }
  }

}
