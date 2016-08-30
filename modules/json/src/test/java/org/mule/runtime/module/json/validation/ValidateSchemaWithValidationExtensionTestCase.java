/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.json.validation;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.extension.validation.api.ValidationException;
import org.mule.extension.validation.api.ValidationExtension;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.exception.MessagingException;

import org.junit.Test;

public class ValidateSchemaWithValidationExtensionTestCase extends ExtensionFunctionalTestCase {

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {ValidationExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "validate-schema-with-validation-module-config.xml";
  }

  @Test
  public void validateInGroup() throws Exception {
    MessagingException e = flowRunner("validate").runExpectingException();
    assertThat(e, is(instanceOf(ValidationException.class)));
  }
}
