/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
