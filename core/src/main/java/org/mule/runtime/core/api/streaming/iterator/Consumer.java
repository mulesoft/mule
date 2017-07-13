/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.streaming.iterator;


import org.mule.runtime.api.streaming.HasSize;
import org.mule.runtime.core.internal.streaming.object.iterator.ClosedConsumerException;

import java.io.Closeable;
import java.util.NoSuchElementException;

/**
 * General interface for components able to consume data from any specific resource or stream, following the Producer-Consumer
 * design pattern. Implementing this interface does not guarantee thread safeness. Check each particular implementation for
 * information about that
 * 
 * @since 3.5.0
 */
public interface Consumer<T> extends Closeable, HasSize {

  /**
   * Retrieves the next available item.
   * 
   * @return an object of type T if available. <code>null</code> otherwise
   * @throws {@link ClosedConsumerException} if the consumer is already closed
   */
  T consume() throws NoSuchElementException;

  /**
   * Returns <code>true</code> if no more items are available or if the consumer was closed. When this method returns
   * <code>true</code>, implementors of this class are require to invoke the {@link Closeable#close()} method before returning in
   * order to release resources as quickly as possible. Users of this component who no longer need this require before it is fully
   * consumed are also required to close it.
   * 
   * @return <code>true</code> if no more items are available. <code>false</code> otherwise
   */
  boolean isConsumed();

}
