/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.routing.split;

import org.mule.runtime.core.internal.routing.split.AbstractMessageSequence;

public class ArrayMessageSequence extends AbstractMessageSequence<Object> {

  private final Object[] array;
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


