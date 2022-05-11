/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;


import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsIterableContaining.hasItem;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static java.util.Collections.synchronizedSet;
import static org.mule.runtime.core.internal.util.rx.ReactorTransactionUtils.TX_SCOPES_KEY;
import static org.mule.runtime.core.internal.util.rx.ReactorTransactionUtils.popTxFromSubscriberContext;
import static org.mule.runtime.core.internal.util.rx.ReactorTransactionUtils.pushTxToSubscriberContext;
import static reactor.util.context.Context.empty;

import org.mule.runtime.api.tx.TransactionException;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

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

  @Issue("MULE-19937")
  @Test
  public void returnsNewSinkWhenInTxByContext() {
    Context ctx = pushTxToSubscriberContext("location").apply(empty());
    FluxSink supplied = txSupplier.get(ctx);
    // Assert is another sink
    assertThat(supplied, is(not(sameInstance(mockSink))));
    // Assert that supplied for same thread supplies same sink
    assertThat(txSupplier.get(ctx), is(sameInstance(supplied)));
  }

  @Issue("MULE-19937")
  @Test
  public void txIsHandledCorrectlyInContext() {
    Context ctx = pushTxToSubscriberContext("location").apply(empty());
    assertThat(ctx.<Deque<String>>get(TX_SCOPES_KEY), hasItem("location"));
    ctx = popTxFromSubscriberContext().apply(ctx);
    assertThat(ctx.<Deque<String>>get(TX_SCOPES_KEY), not(hasItem("location")));
  }

  @Test
  public void newSinkPerThread() throws Exception {
    List<Thread> threads = new ArrayList<>();
    Set<FluxSink> sinks = synchronizedSet(new HashSet<>());
    for (int i = 0; i < THREAD_TEST; i++) {
      Thread thread = new Thread(() -> {
        Transaction tx = mock(Transaction.class);
        try {
          try {
            TransactionCoordination.getInstance().bindTransaction(tx);
            FluxSink sink = txSupplier.get();
            // Assert that supplied for same thread supplies same sink
            assertThat(txSupplier.get(), is(sameInstance(sink)));
            sinks.add(sink);
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

  @Issue("MULE-19937")
  @Test
  public void newSinkPerThreadWithContext() throws Exception {
    Set<FluxSink> sinks = synchronizedSet(new HashSet<>());
    Context ctx = pushTxToSubscriberContext("location").apply(empty());

    ExecutorService executorService = newFixedThreadPool(THREAD_TEST);
    try {
      for (int i = 0; i < THREAD_TEST; i++) {
        executorService.submit(() -> {
          FluxSink sink = txSupplier.get(ctx);
          sinks.add(sink);
        });
      }
    } finally {
      executorService.shutdown();
    }

    executorService.awaitTermination(5, SECONDS);

    // Every thread created has a different sink
    assertThat(sinks, hasSize(THREAD_TEST));
  }

}
