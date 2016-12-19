/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connector;

import org.junit.rules.ExpectedException;
import org.mule.functional.junit4.InvalidExtensionConfigTestCase;
import org.mule.test.petstore.extension.PetStoreConnector;

public class PetStoreMissingRequiredParameterInConfigTestCase extends InvalidExtensionConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "petstore-missing-required-parameter-in-config.xml";
  }

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {PetStoreConnector.class};
  }

  @Override
  protected void additionalExceptionAssertions(ExpectedException expectedException) {
    expectedException
        .expectMessage("Parameter 'pets' of type java.util.List from the configuration 'config' is required but wasn't set");
  }
}
