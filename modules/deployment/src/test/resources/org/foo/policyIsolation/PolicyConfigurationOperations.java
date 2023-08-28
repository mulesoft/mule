/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.policyIsolation;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;

public class PolicyConfigurationOperations {

  public void checkExplicitConfiguration(@Config PolicyConfigurationExtension config) {
    if (config.getConfigurationParameter().equals(PolicyConfigurationExtension.CONFIG_PARAMETER_DEFAULT_VALUE)) {
      throw new IllegalStateException("Operation explicit config should not contain the default configuration");
    }
  }

  public void checkImplicitConfiguration(@Config PolicyConfigurationExtension config) {
    if (!config.getConfigurationParameter().equals(PolicyConfigurationExtension.CONFIG_PARAMETER_DEFAULT_VALUE)) {
      throw new IllegalStateException("Operation implicit config should contain the default configuration");
    }
  }

}
