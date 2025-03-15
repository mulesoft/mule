/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.core.api.event.CoreEvent;
import org.reactivestreams.Publisher;

import javax.lang.model.util.Types;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectSinkTest {

  private DirectSink sink;
  @Mock
  private Function<Publisher<CoreEvent>, Publisher<CoreEvent>> function;
  @Mock
  private Consumer<CoreEvent> eventConsumer;
  @Captor
  private ArgumentCaptor<Publisher<CoreEvent>> publisherCaptor;

  @BeforeEach
  void setUp() {
    when(function.apply(any())).thenAnswer(inv -> inv.getArgument(0));

    sink = new DirectSink(function, eventConsumer, 2);
  }

  @Test
  void dispose() {
    sink.dispose();

    verify(function).apply(publisherCaptor.capture());
  }
}
