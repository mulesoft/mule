/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import static java.util.Objects.requireNonNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

import reactor.core.publisher.FluxSink;

/**
 * Publisher source which emits an unlimited number of items.
 * <p>
 * Additionally, the items are served from a different thread, simulating a parallel source.
 *
 * @param <T> the type of items to emit.
 */
class InfiniteEmitter<T> implements Consumer<FluxSink<T>> {

  /**
   * The items supplier interface, which is like a regular {@link Supplier}, except this one can throw {@link Exception}.
   *
   * @param <ItemType> the item type.
   */
  public interface ItemSupplier<ItemType> {

    ItemType getItem() throws Exception;
  }

  private FluxSink<T> sink;
  private final Thread producerThread;
  private boolean stopRequested = false;

  /**
   * Creates the emitter instance, which will take each item from the given {@code itemSupplier}.
   *
   * @param itemSupplier the supplier for each of the items in the unlimited sequence.
   */
  public InfiniteEmitter(ItemSupplier<T> itemSupplier) {
    producerThread = new Thread(() -> {
      while (!stopRequested) {
        try {
          sink.next(itemSupplier.getItem());
        } catch (Exception e) {
          sink.error(e);
        }
      }
    });
  }

  @Override
  public void accept(FluxSink<T> sink) {
    this.sink = sink;
  }

  /**
   * Starts the emission of items.
   * <p>
   * Note: it needs to have been accepted by a publisher beforehand.
   */
  public void start() {
    requireNonNull(sink, "Attempted to start an unsubscribed source");
    producerThread.start();
  }

  /**
   * Stops the emission of items and signals the completion of the source.
   * <p>
   * Note: it does not support restarting.
   */
  public void stop() throws InterruptedException {
    stopRequested = true;
    producerThread.join();
    sink.complete();
  }
}
