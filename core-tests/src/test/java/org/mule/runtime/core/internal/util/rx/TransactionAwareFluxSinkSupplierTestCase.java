/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import reactor.core.publisher.FluxSink;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionAwareFluxSinkSupplierTestCase {

  private static final int THREAD_TEST = 10;

  private FluxSinkSupplier mockSupplier = mock(FluxSinkSupplier.class);
  private FluxSink mockSink = mock(FluxSink.class);
  private FluxSinkSupplier<FluxSink> txSupplier;

  @Before
  public void setUp() {
    when(mockSupplier.get()).thenReturn(mockSink);
    txSupplier = new TransactionAwareFluxSinkSupplier(() -> mock(FluxSink.class), mockSupplier);
  }

  @Test
  public void delegatesWhenNotInTx() {
    assertThat(txSupplier.get(), is(mockSink));
    // Assert that it doesn't change between calls
    assertThat(txSupplier.get(), is(mockSink));
  }

  @Test
  public void returnsNewSinkWhenInTx() throws TransactionException {
    Transaction tx = mock(Transaction.class);
    try {
      TransactionCoordination.getInstance().bindTransaction(tx);
      FluxSink supplied = txSupplier.get();
      // Assert is another sink
      assertThat(supplied, is(not(mockSink)));
      // Assert that supplied for same thread supplies same sink
      assertThat(txSupplier.get(), is(supplied));
    } finally {
      TransactionCoordination.getInstance().unbindTransaction(tx);
    }
  }

  @Test
  public void newSinkPerThread() throws Exception {
    List<Thread> threads = new ArrayList<>();
    Set<FluxSink> sinks = new HashSet<>();
    for (int i = 0; i < THREAD_TEST; i++) {
      Thread thread = new Thread(() -> {
        Transaction tx = mock(Transaction.class);
        try {
          try {
            TransactionCoordination.getInstance().bindTransaction(tx);
            FluxSink sink = txSupplier.get();
            // Assert that supplied for same thread supplies same sink
            assertThat(txSupplier.get(), is(sink));
            synchronized (sinks) {
              sinks.add(sink);
            }
          } finally {
            TransactionCoordination.getInstance().unbindTransaction(tx);
          }
        } catch (TransactionException e) {

        }
      });
      threads.add(thread);
    }

    for (Thread t : threads) {
      t.start();
    }
    for (Thread t : threads) {
      t.join();
    }

    // Every thread created has a different sink
    assertThat(sinks, hasSize(THREAD_TEST));
  }

}
