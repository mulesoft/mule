/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.streaming.iterator;


import org.mule.runtime.api.streaming.HasSize;

import java.io.Closeable;

/**
 * A producer implementation that follows the idea of the Producer-Consumer design pattern. Implementing this interface does not
 * guarantee thread safeness. Check each particular implementation for information about that
 * 
 * @since 3.5.0
 */
public interface Producer<T> extends Closeable, HasSize {

  /**
   * Returns the next available item
   * 
   * @return an item. Might be <code>null<c/code>
   */
  T produce();

}
