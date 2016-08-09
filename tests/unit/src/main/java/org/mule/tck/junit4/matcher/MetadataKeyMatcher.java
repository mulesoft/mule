/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.matcher;

import static java.lang.String.format;

import org.mule.runtime.api.metadata.MetadataKey;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * {@link Matcher} implementation for {@link MetadataKey}
 *
 * @since 4.0
 */
final public class MetadataKeyMatcher extends TypeSafeMatcher<MetadataKey> {

  private final String id;
  private String displayName;
  private String partName;
  private StringBuilder descriptionBuilder = new StringBuilder();

  private MetadataKeyMatcher(String id) {
    this.id = id;
    descriptionBuilder.append(String.format("a MetadataKey with id: '%s'", id));
  }

  /**
   * Creates a new instance of the {@link MetadataKeyMatcher}
   *
   * @param id of the {@link MetadataKey}
   * @return the new instance of {@link MetadataKeyMatcher}
   */
  public static MetadataKeyMatcher metadataKeyWithId(String id) {
    return new MetadataKeyMatcher(id);
  }

  @Override
  protected boolean matchesSafely(MetadataKey metadataKey) {
    try {
      validateEquals(id, metadataKey.getId());
      validateEquals(displayName, metadataKey.getDisplayName());
      validateEquals(partName, metadataKey.getPartName());
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
   * @param displayName of the {@link MetadataKey}
   * @return the contributed {@link MetadataKeyMatcher}
   */

  public MetadataKeyMatcher withDisplayName(String displayName) {
    this.displayName = displayName;
    descriptionBuilder.append(format(", displayName: '%s'", displayName));
    return this;
  }

  /**
   * Adds a partName to compare. If is not added the matcher won't compare partNames
   *
   * @param partName of the {@link MetadataKey}
   * @return the contributed {@link MetadataKeyMatcher}
   */
  public MetadataKeyMatcher withPartName(String partName) {
    this.partName = partName;
    descriptionBuilder.append(format(", partName: '%s'", partName));
    return this;
  }

  private void validateEquals(String actual, String expected) {
    if (actual != null && !expected.equals(actual)) {
      throw new RuntimeException(String.format("Assertion Error - Actual: '%s' Expected: '%s'", actual, expected));
    }
  }
}
