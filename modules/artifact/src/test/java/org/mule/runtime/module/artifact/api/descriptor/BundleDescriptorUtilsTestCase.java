/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorUtils.isCompatibleVersion;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

@SmallTest
public class BundleDescriptorUtilsTestCase extends AbstractMuleTestCase {

  @Test
  public void detectsCompatibleBugFixVersion() throws Exception {
    assertThat(isCompatibleVersion("1.0.1", "1.0.0"), is(true));
  }

  @Test
  public void detectsCompatibleMinorVersion() throws Exception {
    assertThat(isCompatibleVersion("1.1.0", "1.0.0"), is(true));
  }

  @Test
  public void detectsIncompatibleBugFixVersion() throws Exception {
    assertThat(isCompatibleVersion("1.0.0", "1.0.1"), is(false));
  }

  @Test
  public void detectsIncompatibleMinorVersion() throws Exception {
    assertThat(isCompatibleVersion("1.0.0", "1.1.0"), is(false));
  }

  @Test
  public void detectsCompatibleMinorVersionWithNoBugFix() throws Exception {
    assertThat(isCompatibleVersion("1.1", "1.0"), is(true));
  }

  @Test
  public void detectsIncompatibleMinorVersionWithNoBugFix() throws Exception {
    assertThat(isCompatibleVersion("1.0", "1.1"), is(false));
  }

  @Test
  public void detectsIncompatibleMajorVersion() throws Exception {
    assertThat(isCompatibleVersion("2.0.0", "1.0.0"), is(false));
  }

  @Test
  public void detectsIncompatibleMajorVersionWithNoMinor() throws Exception {
    assertThat(isCompatibleVersion("2", "1"), is(false));
  }

  @Test
  public void detectsIncompatibleMajorVersionWithNoBugFix() throws Exception {
    assertThat(isCompatibleVersion("2.0", "1.0"), is(false));
  }
}
