/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.streaming.iterator;


import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.streaming.HasSize;

import java.io.Closeable;

/**
 * A producer implementation that follows the idea of the Producer-Consumer design pattern. Implementing this interface does not
 * guarantee thread safeness. Check each particular implementation for information about that
 * 
 * @since 3.5.0
 */
@NoImplement
public interface Producer<T> extends Closeable, HasSize {

  /**
   * Returns the next available item
   * 
   * @return an item. Might be <code>null<c/code>
   */
  T produce();

}
