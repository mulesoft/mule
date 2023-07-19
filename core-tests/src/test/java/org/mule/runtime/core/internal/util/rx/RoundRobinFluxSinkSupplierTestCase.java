/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.rx;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;

import reactor.core.publisher.FluxSink;

public class RoundRobinFluxSinkSupplierTestCase extends AbstractMuleTestCase {

  private static final int SINKS_COUNT = 10;

  @Test
  public void loopAroundSinks() {
    Supplier factoryMock = mock(Supplier.class);
    when(factoryMock.get()).thenAnswer(invocationOnMock -> mock(FluxSink.class));

    FluxSinkSupplier<FluxSink> roundRobin = new RoundRobinFluxSinkSupplier(SINKS_COUNT, factoryMock);
    List<FluxSink> sinks = new ArrayList<>();

    for (int i = 0; i < SINKS_COUNT; i++) {
      sinks.add(roundRobin.get());
    }
    verify(factoryMock, times(SINKS_COUNT)).get();

    // We got all different sinks
    assertThat(new HashSet<>(sinks), hasSize(SINKS_COUNT));

    // Go for another round and check sinks repeat
    for (int i = 0; i < SINKS_COUNT; i++) {
      assertThat(roundRobin.get(), is(sinks.get(i)));
    }

  }

}
