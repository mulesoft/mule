/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import org.mule.test.module.extension.InvalidExtensionConfigTestCase;

import org.junit.rules.ExpectedException;

public class PetStoreMissingRequiredParameterInsidePojoTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-missing-required-parameter.xml";
  }

  @Override
  protected void additionalExceptionAssertions(ExpectedException expectedException) {
    expectedException
        .expectMessage(is(containsString("The object 'PhoneNumber' requires the parameter 'areaCodes' but is not set")));
  }
}
