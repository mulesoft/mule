/*
 * Copyright 2015 Ben Manes. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package org.mule.runtime.core.internal.concurrent;

import java.util.function.Consumer;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This class is the same as {@link com.github.benmanes.caffeine.cache.Buffer}. Copied here due to visibility restrictions.
 *
 * @param <T> the generic type of the buffer's items
 * @since 4.3.0
 */
public interface CaffeineBuffer<T> {

  int FULL = 1;
  int SUCCESS = 0;
  int FAILED = -1;

  /**
   * Returns a no-op implementation.
   */
  @SuppressWarnings("unchecked")
  static <T> CaffeineBuffer<T> disabled() {
    return (CaffeineBuffer<T>) DisabledBuffer.INSTANCE;
  }

  /**
   * Inserts the specified element into this buffer if it is possible to do so immediately without
   * violating capacity restrictions. The addition is allowed to fail spuriously if multiple
   * threads insert concurrently.
   *
   * @param e the element to add
   * @return {@code 1} if the buffer is full, {@code -1} if the CAS failed, or {@code 0} if added
   */
  int offer(@NonNull T e);

  /**
   * Drains the buffer, sending each element to the consumer for processing. The caller must ensure
   * that a consumer has exclusive read access to the buffer.
   *
   * @param consumer the action to perform on each element
   */
  void drainTo(@NonNull Consumer<T> consumer);

  /**
   * Returns the number of elements residing in the buffer.
   *
   * @return the number of elements in this buffer
   */
  default int size() {
    return writes() - reads();
  }

  /**
   * Returns the number of elements that have been read from the buffer.
   *
   * @return the number of elements read from this buffer
   */
  int reads();

  /**
   * Returns the number of elements that have been written to the buffer.
   *
   * @return the number of elements written to this buffer
   */
  int writes();
}


enum DisabledBuffer implements CaffeineBuffer<Object> {
  INSTANCE;

  @Override
  public int offer(Object e) {
    return SUCCESS;
  }

  @Override
  public void drainTo(Consumer<Object> consumer) {}

  @Override
  public int size() {
    return 0;
  }

  @Override
  public int reads() {
    return 0;
  }

  @Override
  public int writes() {
    return 0;
  }
}
