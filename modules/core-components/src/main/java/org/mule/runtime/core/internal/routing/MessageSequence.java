/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import java.util.Iterator;

/**
 * A sequence of messages
 * 
 * @author flbulgarelli
 * @param <T> the message payload type
 */
public interface MessageSequence<T> extends Iterator<T> {

  Integer UNKNOWN_SIZE = null;

  /**
   * If the sequence is empty
   * 
   * @return !hasNext()
   */
  boolean isEmpty();

  /**
   * The number of members of the sequence. If this is unknown, return UNKNOWN_SIZE.
   * 
   * @return The estimated size of the sequence, or {@link #UNKNOWN_SIZE}, if it is unknown
   */
  Integer size();

  /**
   * Whether this sequence has more elements.
   * 
   * @see Iterator#hasNext()
   */
  @Override
  boolean hasNext();

  /**
   * The next element of the sequence. At any moment, if {@link #size()} is not equal to {@link #UNKNOWN_SIZE}, this means that
   * this method may be invoked approximately up to {@link #size()} times.
   */
  @Override
  T next();

  /**
   * Unsupported operation. {@link MessageSequence} do not allow removal of elements.
   */
  @Override
  void remove();
}
