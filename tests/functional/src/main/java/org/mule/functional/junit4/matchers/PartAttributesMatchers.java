/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4.matchers;

import org.mule.runtime.core.api.message.PartAttributes;

import org.hamcrest.Factory;
import org.hamcrest.Matcher;

public class PartAttributesMatchers {

  /**
   * Verifies the name of a part.
   */
  @Factory
  public static Matcher<PartAttributes> hasName(String name) {
    return new IsPartWithName(name);
  }

  /**
   * Verifies the filename of a part.
   */
  @Factory
  public static Matcher<PartAttributes> hasFilename(String filename) {
    return new IsPartWithFilename(filename);
  }

  /**
   * Verifies the size of a part.
   */
  @Factory
  public static Matcher<PartAttributes> hasSize(long size) {
    return new IsPartWithSize(size);
  }
}
