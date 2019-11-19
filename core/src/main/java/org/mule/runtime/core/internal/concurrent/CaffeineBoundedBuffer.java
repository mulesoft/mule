/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.concurrent;


import static org.mule.runtime.core.internal.concurrent.CaffeineBoundedBuffer.*;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

import com.github.benmanes.caffeine.base.UnsafeAccess;

public class CaffeineBoundedBuffer<T> extends CaffeineStripedBuffer<T> {

  /*
   * A circular ring buffer stores the elements being transferred by the producers to the consumer.
   * The monotonically increasing count of reads and writes allow indexing sequentially to the next
   * element location based upon a power-of-two sizing.
   *
   * The producers race to read the counts, check if there is available capacity, and if so then try
   * once to CAS to the next write count. If the increment is successful then the producer lazily
   * publishes the element. The producer does not retry or block when unsuccessful due to a failed
   * CAS or the buffer being full.
   *
   * The consumer reads the counts and takes the available elements. The clearing of the elements
   * and the next read count are lazily set.
   *
   * This implementation is striped to further increase concurrency by rehashing and dynamically
   * adding new buffers when contention is detected, up to an internal maximum. When rehashing in
   * order to discover an available buffer, the producer may retry adding its element to determine
   * whether it found a satisfactory buffer or if resizing is necessary.
   */

  /** The maximum number of elements per buffer. */
  static final int BUFFER_SIZE = 16;

  // Assume 4-byte references and 64-byte cache line (16 elements per line)
  static final int SPACED_SIZE = BUFFER_SIZE << 4;
  static final int SPACED_MASK = SPACED_SIZE - 1;
  static final int OFFSET = 16;

  @Override
  protected CaffeineBuffer<T> create(T e) {
    return new RingBuffer<>(e);
  }

  static final class RingBuffer<T> extends BBHeader.ReadAndWriteCounterRef implements CaffeineBuffer<T> {
    final AtomicReferenceArray<T> buffer;

    @SuppressWarnings({"unchecked", "cast", "rawtypes"})
    public RingBuffer(T e) {
      buffer = new AtomicReferenceArray<>(SPACED_SIZE);
      buffer.lazySet(0, e);
    }

    @Override
    public int offer(T e) {
      long head = readCounter;
      long tail = relaxedWriteCounter();
      long size = (tail - head);
      if (size >= SPACED_SIZE) {
        return CaffeineBuffer.FULL;
      }
      if (casWriteCounter(tail, tail + OFFSET)) {
        int index = (int) (tail & SPACED_MASK);
        buffer.lazySet(index, e);
        return SUCCESS;
      }
      return FAILED;
    }

    @Override
    public void drainTo(Consumer<T> consumer) {
      long head = readCounter;
      long tail = relaxedWriteCounter();
      long size = (tail - head);
      if (size == 0) {
        return;
      }
      do {
        int index = (int) (head & SPACED_MASK);
        T e = buffer.get(index);
        if (e == null) {
          // not published yet
          break;
        }
        buffer.lazySet(index, null);
        consumer.accept(e);
        head += OFFSET;
      } while (head != tail);
      lazySetReadCounter(head);
    }

    @Override
    public int reads() {
      return (int) readCounter / OFFSET;
    }

    @Override
    public int writes() {
      return (int) writeCounter / OFFSET;
    }
  }
}

/** The namespace for field padding through inheritance. */
final class BBHeader {

  @SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")
  abstract static class PadReadCounter {
    long p00, p01, p02, p03, p04, p05, p06, p07;
    long p10, p11, p12, p13, p14, p15, p16;
  }

  /** Enforces a memory layout to avoid false sharing by padding the read count. */
  abstract static class ReadCounterRef extends PadReadCounter {
    static final long READ_OFFSET =
        UnsafeAccess.objectFieldOffset(ReadCounterRef.class, "readCounter");

    volatile long readCounter;

    void lazySetReadCounter(long count) {
      UnsafeAccess.UNSAFE.putOrderedLong(this, READ_OFFSET, count);
    }
  }

  abstract static class PadWriteCounter extends ReadCounterRef {
    long p20, p21, p22, p23, p24, p25, p26, p27;
    long p30, p31, p32, p33, p34, p35, p36;
  }

  /** Enforces a memory layout to avoid false sharing by padding the write count. */
  abstract static class ReadAndWriteCounterRef extends PadWriteCounter {
    static final long WRITE_OFFSET =
        UnsafeAccess.objectFieldOffset(ReadAndWriteCounterRef.class, "writeCounter");

    volatile long writeCounter;

    ReadAndWriteCounterRef() {
      UnsafeAccess.UNSAFE.putOrderedLong(this, WRITE_OFFSET, OFFSET);
    }

    long relaxedWriteCounter() {
      return UnsafeAccess.UNSAFE.getLong(this, WRITE_OFFSET);
    }

    boolean casWriteCounter(long expect, long update) {
      return UnsafeAccess.UNSAFE.compareAndSwapLong(this, WRITE_OFFSET, expect, update);
    }
  }
}
