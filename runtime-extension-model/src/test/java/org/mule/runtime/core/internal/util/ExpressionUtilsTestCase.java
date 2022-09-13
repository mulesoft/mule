/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.core.internal.util.ExpressionUtils.isExpression;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ExpressionUtilsTestCase {

  @Test
  public void emptyExpression() {
    assertThat(isExpression("#[]"), is(true));
  }

  @Test
  public void emptyString() {
    assertThat(isExpression(""), is(false));
  }

  @Test
  public void nullValue() {
    assertThat(isExpression(null), is(false));
  }

  @Test
  public void onlyPrefix() {
    assertThat(isExpression("#[hello"), is(false));
  }

  @Test
  public void onlySuffix() {
    assertThat(isExpression("world]"), is(false));
  }

  @Test
  public void helloWorld() {
    assertThat(isExpression("#[Hello world]"), is(true));
  }

  @Test
  public void extraSpaces() {
    assertThat(isExpression("   #[Hello world]  "), is(true));
  }
}
