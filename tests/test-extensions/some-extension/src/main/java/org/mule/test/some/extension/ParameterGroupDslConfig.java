/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

@Configuration(name = "dsl-config")
@Operations(ParameterGroupDslOps.class)
public class ParameterGroupDslConfig {

  @ParameterGroup(name = "parameter-group-dsl", showInDsl = true)
  SomeParameterGroupOneRequiredConfig someParameterGroup;

  public SomeParameterGroupOneRequiredConfig getSomeParameterGroup() {
    return someParameterGroup;
  }
}
