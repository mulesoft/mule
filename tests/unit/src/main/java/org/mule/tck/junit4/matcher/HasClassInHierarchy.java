/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.junit4.matcher;

import org.hamcrest.Description;
import org.hamcrest.DiagnosingMatcher;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;


/**
 * Tests whether the value has a given class name in the class hierarchy.
 * <p>
 * Similar to {@link org.hamcrest.core.IsInstanceOf} but useful when there is not access to the class that has to be matched,
 * for example, when the class is not exposed on the API.
 */
public class HasClassInHierarchy extends DiagnosingMatcher<Object> {

  private final String expectedClassName;

  private HasClassInHierarchy(String expectedClassName) {
    this.expectedClassName = expectedClassName;
  }

  @Override
  protected boolean matches(Object item, Description mismatch) {
    if (null == item) {
      mismatch.appendText("null");
      return false;
    }

    Class currentClass = item.getClass();
    StringBuilder builder = new StringBuilder();
    do {
      if (expectedClassName.equals(currentClass.getName())) {
        return true;
      }
      builder.append(currentClass.getName()).append("\n");
      currentClass = currentClass.getSuperclass();
    } while (currentClass != null);

    mismatch.appendText(builder.toString());

    return false;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a value with a class hierarchy containing ").appendText(expectedClassName);
  }

  @SuppressWarnings("unchecked")
  @Factory
  /**
   * Creates a matcher that will check if a given object has a given class name in the hierarchy.
   *
   * @param expectedClassName class name to check.
   */
  public static <T> Matcher<T> withClassName(String expectedClassName) {
    return (Matcher<T>) new HasClassInHierarchy(expectedClassName);
  }

}
