/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_MEL_AS_DEFAULT;
import static org.mule.runtime.core.internal.el.ExpressionLanguageAdaptorHandler.MVEL_NOT_INSTALLED_ERROR;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ExpressionLanguageAdaptorHandlerTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void checksThatMelIsInstalledWhenDefault() throws Exception {
    testWithSystemProperty(MULE_MEL_AS_DEFAULT, "true", () -> {
      expectedException.expect(IllegalStateException.class);
      expectedException.expectMessage(MVEL_NOT_INSTALLED_ERROR);
      new ExpressionLanguageAdaptorHandler(mock(ExtendedExpressionLanguageAdaptor.class), null);
    });
  }

  @Test
  public void doesNotCheckThatMelIsInstalledWhenNotDefault() throws Exception {
    testWithSystemProperty(MULE_MEL_AS_DEFAULT, "false", () -> {
      new ExpressionLanguageAdaptorHandler(mock(ExtendedExpressionLanguageAdaptor.class), null);
    });
  }
}
