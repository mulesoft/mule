/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing.outbound;

import org.mule.runtime.core.internal.routing.AbstractMessageSequence;
import org.mule.runtime.core.internal.routing.MessageSequence;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

/**
 * A {@link MessageSequence} that delegates its {@link #hasNext()} and {@link #next()} methods to an {@link Iterator}, and has no
 * estimated size
 * 
 * @author flbulgarelli
 * @param <T>
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
