/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
