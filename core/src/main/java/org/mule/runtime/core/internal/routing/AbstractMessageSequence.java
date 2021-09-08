/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

/**
 * An abstract implementation of a {@link MessageSequence}, that does not support {@link #remove()}
 * 
 * @author flbulgarelli
 * @param <PayloadType>
 */
public abstract class AbstractMessageSequence<PayloadType> implements MessageSequence<PayloadType> {

  public final boolean isEmpty() {
    return !hasNext();
  }

  public final void remove() {
    throw new UnsupportedOperationException();
  }
}
