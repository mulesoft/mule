/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
