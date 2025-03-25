/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

class AbstrctStreamProcessingStrategyFactoryTestCase {

  private AbstractStreamProcessingStrategyFactory factory;

  @BeforeEach
  void setUp() {
    factory = mock(AbstractStreamProcessingStrategyFactory.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void setSubscriberCount() {
    factory.setSubscriberCount(2);
    assertThat(factory.getSubscriberCount(), is(2));
  }

  /**
   * Buffer size has to be a power of 2. Do you believe in the power of 2? Of course you do...
   */
  @Test
  void setIllegalBufferSize() {
    assertThrows(IllegalArgumentException.class, () -> factory.setBufferSize(3));
  }

  @Test
  void setBufferSize() {
    factory.setBufferSize(2);
    assertThat(factory.getBufferSize(), is(2));
  }

}
