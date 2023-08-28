/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing.outbound;

import org.mule.runtime.core.internal.routing.split.AbstractMessageSequence;
import org.mule.runtime.core.internal.routing.split.MessageSequence;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

/**
 * A {@link MessageSequence} that delegates its {@link #hasNext()} and {@link #next()} methods to an {@link Iterator}, and has no
 * estimated size
 * 
 * @author flbulgarelli
 */
public final class IteratorMessageSequence extends AbstractMessageSequence {

  private final Iterator iter;

  public IteratorMessageSequence(Iterator iter) {
    Validate.notNull(iter);
    this.iter = iter;
  }

  @Override
  public Integer size() {
    return UNKNOWN_SIZE;
  }

  @Override
  public boolean hasNext() {
    return iter.hasNext();
  }

  @Override
  public Object next() {
    if (iter instanceof EventBuilderConfigurerIterator) {
      return ((EventBuilderConfigurerIterator) iter).nextEventBuilderConfigurer();
    } else {
      return iter.next();
    }
  }

}
