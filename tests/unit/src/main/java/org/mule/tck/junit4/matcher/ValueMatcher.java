/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import org.mule.runtime.api.value.Value;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * {@link Matcher} to easily test the structure of a {@link Value}
 *
 * @since 4.0
 */
public final class ValueMatcher extends TypeSafeMatcher<Value> {


  private final String id;
  private String displayName;
  private String partName;
  private StringBuilder descriptionBuilder = new StringBuilder();
  private ValueMatcher[] valueMatchers;

  private ValueMatcher(String id) {
    this.id = id;
    descriptionBuilder.append(String.format("a Value with id: '%s'", id));
  }

  /**
   * Creates a new instance of the {@link ValueMatcher}
   *
   * @param id of the {@link Value}
   * @return the new instance of {@link ValueMatcher}
   */
  public static ValueMatcher valueWithId(String id) {
    return new ValueMatcher(id);
  }

  @Override
  protected boolean matchesSafely(Value value) {
    try {
      validateEquals(id, value.getId());
      validateEquals(displayName, value.getDisplayName());
      validateEquals(partName, value.getPartName());
      if (valueMatchers != null) {
        return hasItems(valueMatchers).matches(value.getChilds());
      }
      return true;
    } catch (RuntimeException e) {
      return false;
    }
  }

  @Override
  public void describeTo(Description description) {
    description.appendText(descriptionBuilder.toString());
  }

  /**
   * Adds a displayName to compare. If is not added the matcher won't compare displayNames
   *
   * @param displayName of the {@link Value}
   * @return the contributed {@link ValueMatcher}
   */

  public ValueMatcher withDisplayName(String displayName) {
    this.displayName = displayName;
    descriptionBuilder.append(format(", displayName: '%s'", displayName));
    return this;
  }

  /**
   * Adds a partName to compare. If is not added the matcher won't compare partNames
   *
   * @param partName of the {@link Value}
   * @return the contributed {@link ValueMatcher}
   */
  public ValueMatcher withPartName(String partName) {
    this.partName = partName;
    descriptionBuilder.append(format(", partName: '%s'", partName));
    return this;
  }

  /**
   * Adds the {@link Value} parts to compare. If is not added the matcher won't compare the childs.
   *
   * @param valueMatcher child's matchers
   * @return the contribute {@link ValueMatcher}
   */
  public ValueMatcher withChilds(ValueMatcher... valueMatcher) {
    this.valueMatchers = valueMatcher;
    descriptionBuilder.append(format(", with child values: [%s]", stream(valueMatchers)
        .map(Matcher::toString)
        .collect(joining(", "))));
    return this;
  }

  private void validateEquals(String actual, String expected) {
    if (actual != null && !expected.equals(actual)) {
      throw new RuntimeException(String.format("Assertion Error - Actual: '%s' Expected: '%s'", actual, expected));
    }
  }
}
