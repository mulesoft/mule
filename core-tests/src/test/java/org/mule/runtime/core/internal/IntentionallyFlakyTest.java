/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;

public class IntentionallyFlakyTest {

  @Test
  public void flaky1() {
    double random = Math.random();
    assertThat(random, Matchers.lessThan(0.5));
  }

  @Test
  public void flaky2() {
    double random = Math.random();
    assertThat(random, Matchers.lessThan(0.5));
  }

  @Test
  public void flaky3() {
    double random = Math.random();
    assertThat(random, Matchers.lessThan(0.5));
  }

  @Test
  public void flaky4() {
    double random = Math.random();
    assertThat(random, Matchers.lessThan(0.5));
  }

  @Test
  public void flaky5() {
    double random = Math.random();
    assertThat(random, Matchers.lessThan(0.5));
  }

  @Test
  public void flaky6() {
    double random = Math.random();
    assertThat(random, Matchers.lessThan(0.5));
  }

}
