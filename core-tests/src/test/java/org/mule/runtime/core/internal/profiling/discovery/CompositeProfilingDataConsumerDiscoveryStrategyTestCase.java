/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.profiling.discovery;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.profiling.ProfilingDataConsumer;
import org.mule.runtime.api.profiling.ProfilingDataConsumerDiscoveryStrategy;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.mule.tck.size.SmallTest;

@SmallTest
public class CompositeProfilingDataConsumerDiscoveryStrategyTestCase {

  private final ProfilingDataConsumer<?> profilingDataConsumerOne = mock(ProfilingDataConsumer.class);
  private final ProfilingDataConsumer<?> profilingDataConsumerTwo = mock(ProfilingDataConsumer.class);
  private final ProfilingDataConsumerDiscoveryStrategy discoveryStrategyOne = mock(ProfilingDataConsumerDiscoveryStrategy.class);
  private final ProfilingDataConsumerDiscoveryStrategy discoveryStrategyTwo = mock(ProfilingDataConsumerDiscoveryStrategy.class);
  private final Set<ProfilingDataConsumerDiscoveryStrategy> discoveryStrategies =
      new HashSet<ProfilingDataConsumerDiscoveryStrategy>() {

        {
          add(discoveryStrategyOne);
          add(discoveryStrategyTwo);
        }
      };

  @Before
  public void setUp() {
    when(discoveryStrategyOne.discover()).thenAnswer(invocation -> Collections.singleton(profilingDataConsumerOne));
    when(discoveryStrategyTwo.discover()).thenAnswer(invocation -> Collections.singleton(profilingDataConsumerTwo));
  }

  @Test
  public void testValues() {
    CompositeProfilingDataConsumerDiscoveryStrategy compositeDiscoveryStrategy =
        new CompositeProfilingDataConsumerDiscoveryStrategy(discoveryStrategies);
    assertThat(compositeDiscoveryStrategy.discover(), containsInAnyOrder(profilingDataConsumerOne, profilingDataConsumerTwo));
  }

}
