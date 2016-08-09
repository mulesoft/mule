/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.launcher.service;

import java.util.function.Predicate;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class PredicateMatcher<T> extends TypeSafeMatcher<T> {

  Predicate<T> predicate;

  public PredicateMatcher(Predicate<T> predicate) {
    this.predicate = predicate;
  }

  public static <T> PredicateMatcher<T> matchesPredicate(Predicate<T> predicate) {
    return new PredicateMatcher<T>(predicate);
  }

  @Override
  protected boolean matchesSafely(T item) {
    return predicate.test(item);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("successfully evaluated predicate");
  }

  @Override
  protected void describeMismatchSafely(T item, Description description) {
    description.appendText("predicate evaluated to false on " + item);
  }
}
