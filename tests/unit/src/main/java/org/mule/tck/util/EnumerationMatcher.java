/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.util;

import java.util.Collection;
import java.util.Enumeration;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matches a {@link Enumeration} against a collection of expected items.
 */
public class EnumerationMatcher<T> extends TypeSafeMatcher<Enumeration<T>> {

  private final Collection<T> items;

  public EnumerationMatcher(Collection<T> items) {
    this.items = items;
  }

  @Override
  public boolean matchesSafely(Enumeration<T> item) {
    int enumerationSize = 0;
    while (item.hasMoreElements()) {
      T currentItem = item.nextElement();

      enumerationSize++;

      if (!items.contains(currentItem)) {
        return false;
      }
    }

    return items.size() == enumerationSize;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("an Enumeration containing " + items.toString());
  }

  public static <T> Matcher<Enumeration<T>> equalTo(Collection<T> items) {
    return new EnumerationMatcher<>(items);
  }
}
