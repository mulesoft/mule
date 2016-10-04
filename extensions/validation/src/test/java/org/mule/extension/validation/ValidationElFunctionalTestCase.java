/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.validation;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.Event;

import org.junit.Test;

public class ValidationElFunctionalTestCase extends ValidationTestCase {

  @Override
  protected String getConfigFile() {
    return "expression-language-validator.xml";
  }

  @Test
  public void byClassInstanceReused() throws Exception {
    final Event response = flowRunner("validationExpressionLanguage").withPayload("foo@bar.com").run();

    assertThat(response.getError().isPresent(), is(false));
  }
}
