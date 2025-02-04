/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.version;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class VersionRangeTestCase extends AbstractMuleTestCase {

  @Test
  public void allVersions() {
    assertRangeContainsVersion(new VersionRange("[,]"), "1.0.0", true);
  }

  @Test
  public void allVersionsIncludes() {
    assertRangeContainsVersion(new VersionRange("(,)"), "1.0.0", true);
  }

  @Test
  public void lowerIncludes() {
    assertRangeContainsVersion(new VersionRange("[1.0.0,]"), "1.0.0", true);
  }

  @Test
  public void lowerExcludes() {
    assertRangeContainsVersion(new VersionRange("(1.0.0,]"), "1.0.0", false);
  }

  @Test
  public void lowerIncludesBelow() {
    assertRangeContainsVersion(new VersionRange("[2.0.0,]"), "1.0.0", false);
  }

  @Test
  public void lowerExcludesBelow() {
    assertRangeContainsVersion(new VersionRange("(2.0.0,]"), "1.0.0", false);
  }

  @Test
  public void higherIncludes() {
    assertRangeContainsVersion(new VersionRange("[,1.0.0]"), "1.0.0", true);
  }

  @Test
  public void higherExcludes() {
    assertRangeContainsVersion(new VersionRange("(,1.0.0)"), "1.0.0", false);
  }

  @Test
  public void higherIncludesAbove() {
    assertRangeContainsVersion(new VersionRange("[,2.0.0]"), "3.0.0", false);
  }

  @Test
  public void higherExcludesAbove() {
    assertRangeContainsVersion(new VersionRange("(,2.0.0]"), "3.0.0", false);
  }

  private void assertRangeContainsVersion(final VersionRange range, String version, boolean expected) {
    assertThat("`" + version + "` " + (expected ? "not " : " ") + "contained in " + range.toString(),
               range.contains(version), is(expected));
  }
}
