/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.core.internal.routing.AbstractMessageSequence;

public class ArrayMessageSequence extends AbstractMessageSequence<Object> {

  private Object[] array;
  private int idx;

  public ArrayMessageSequence(Object[] array) {
    this.array = array;
    idx = 0;
  }

  @Override
  public Integer size() {
    return array.length - idx;
  }

  @Override
  public boolean hasNext() {
    return idx < array.length;
  }

  @Override
  public Object next() {
    return array[idx++];
  }

}


