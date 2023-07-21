/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util;

import static org.mule.runtime.core.internal.expression.util.ExpressionUtils.isExpression;

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
