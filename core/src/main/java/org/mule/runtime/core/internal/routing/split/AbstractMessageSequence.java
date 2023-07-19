/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing.split;

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
