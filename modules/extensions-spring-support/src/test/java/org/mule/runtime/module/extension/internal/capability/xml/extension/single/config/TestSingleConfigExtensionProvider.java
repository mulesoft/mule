/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.single.config;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

/**
 * Provider description
 */
public class TestSingleConfigExtensionProvider implements ConnectionProvider<String> {

  /**
   * Connection Param Description
   */
  @Parameter
  private String connectionParam1;

  @org.mule.sdk.api.annotation.param.ParameterGroup(name = "Connection Group")
  private SingleConfigParameterGroup group;

  @Override
  public String connect() throws ConnectionException {
    return "";
  }

  @Override
  public void disconnect(String connection) {

  }

  @Override
  public ConnectionValidationResult validate(String connection) {
    return null;
  }
}
