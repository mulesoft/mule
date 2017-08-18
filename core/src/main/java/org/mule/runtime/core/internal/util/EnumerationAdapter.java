/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Creates an {@link Enumeration} containing all the items in a {@link Collection}
 */
public class EnumerationAdapter<T> implements Enumeration<T> {

  private final List<T> items;
  private final Iterator<T> urlIterator;

  public EnumerationAdapter(Collection<T> items) {
    this.items = new LinkedList<>(items);
    this.urlIterator = items.iterator();
  }

  @Override
  public boolean hasMoreElements() {
    return urlIterator.hasNext();
  }

  @Override
  public T nextElement() {
    return urlIterator.next();
  }

  @Override
  public String toString() {
    return items.toString();
  }
}
