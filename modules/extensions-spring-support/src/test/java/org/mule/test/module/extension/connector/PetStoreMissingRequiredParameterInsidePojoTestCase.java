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

public class PetStoreMissingRequiredParameterInsidePojoTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-missing-required-parameter.xml";
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException
        .expectMessage(is(containsString("Element <petstore:phone-number> in line 17 of file petstore-missing-required-parameter.xml is missing required parameter 'area-codes'")));
  }
}
