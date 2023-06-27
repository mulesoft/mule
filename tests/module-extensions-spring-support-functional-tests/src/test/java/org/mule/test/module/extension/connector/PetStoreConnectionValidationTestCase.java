/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.connector;

import static org.junit.rules.ExpectedException.none;
import org.mule.test.module.extension.config.PetStoreConnectionTestCase;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

public class PetStoreConnectionValidationTestCase extends PetStoreConnectionTestCase {

  @Rule
  public ExpectedException expectedEx = none();

  @Override
  protected String getConfigFile() {
    return "petstore-simple-connection.xml";
  }

}
