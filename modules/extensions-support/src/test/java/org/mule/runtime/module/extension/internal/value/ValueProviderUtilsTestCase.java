/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.sanitizeExpression;

import org.junit.Test;

public class ValueProviderUtilsTestCase {

  @Test
  public void sanitizeSimpleName() {
    String sanitized = sanitizeExpression("simple");
    assertThat(sanitized, equalTo("simple_"));
  }

  @Test
  public void sanitizeSimpleExpression() {
    String sanitized = sanitizeExpression("one.two.three");
    assertThat(sanitized, equalTo("one_.'two'.'three'"));
  }

  @Test
  public void sanitizeExpressionWithAttribute() {
    String sanitized = sanitizeExpression("one.two.@three");
    assertThat(sanitized, equalTo("one_.'two'.@'three'"));
  }

  @Test
  public void sanitizeAlreadySanitizedExpression() {
    String sanitized = sanitizeExpression("one.'two'.'three'");
    assertThat(sanitized, equalTo("one_.'two'.'three'"));
  }
}
