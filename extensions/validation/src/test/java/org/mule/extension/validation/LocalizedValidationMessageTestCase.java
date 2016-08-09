/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class LocalizedValidationMessageTestCase extends ValidationTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "validation-i18n.xml";
  }

  @Test
  public void localizedMessage() throws Exception {
    expectedException.expectMessage("Se esperaba que el valor fuera true pero fue false");
    flowRunner("localizedMessage").run();
  }

}
