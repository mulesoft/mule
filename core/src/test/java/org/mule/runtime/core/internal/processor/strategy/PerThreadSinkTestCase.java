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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Sink;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * Check some error handling.
 */
@ExtendWith(MockitoExtension.class)
class PerThreadSinkTestCase {

  private PerThreadSink sink;
  @Mock
  private CoreEvent event;
  @Mock
  private Supplier<Sink> sinkSupplier;


  @BeforeEach
  void setUp() {
    sink = new PerThreadSink(sinkSupplier);
    when(sinkSupplier.get()).thenAnswer(inv -> {
      Sneak.sneakyThrow(new ExecutionException(new NullPointerException("Ho ho! <chomp> <chomp>")));
      return null;
    });
  }

  @Test
  void acceptWithError() {

    assertThrows(IllegalStateException.class, () -> sink.accept(event));
  }

  @Test
  void emitWithError() {

    assertThrows(IllegalStateException.class, () -> sink.emit(event));
  }

  private static class Sneak {

    static void sneakyThrow(Throwable t) {
      Sneak.<RuntimeException>innerSneak(t);
    }

    private static <T extends Throwable> T innerSneak(Throwable t) throws T {
      throw (T) t;
    }
  }
}
