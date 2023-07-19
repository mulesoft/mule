/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.config;

import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;

public class PetStoreSimpleConnectionTestCase extends PetStoreConnectionTestCase {

  @Rule
  public SystemProperty configNameProperty = new SystemProperty("configName", DEFAULT_CONFIG_NAME);

  @Override
  protected String getConfigFile() {
    return "petstore-simple-connection.xml";
  }
}
