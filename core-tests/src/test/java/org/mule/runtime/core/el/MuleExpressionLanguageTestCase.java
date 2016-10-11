/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.core.el.v2.MuleExpressionLanguage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MuleExpressionLanguageTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void nullExpressionExecutor() throws Exception {
    ExpressionLanguage expressionLanguage = new MuleExpressionLanguage();
    expectedException.expect(NullPointerException.class);
    expressionLanguage.evaluate("hey", BindingContext.builder().build());
  }

}
