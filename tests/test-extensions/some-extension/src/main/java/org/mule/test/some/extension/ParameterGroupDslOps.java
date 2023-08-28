/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.param.Config;

public class ParameterGroupDslOps {

  public ParameterGroupDslConfig retrieveConfigurationDsl(@Config ParameterGroupDslConfig config) {
    return config;
  }
}
