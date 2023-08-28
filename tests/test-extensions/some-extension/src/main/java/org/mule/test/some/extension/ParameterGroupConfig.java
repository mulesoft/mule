/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.some.extension;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

import java.time.ZonedDateTime;

@Configuration(name = "config")
@Operations({SomeOps.class})
public class ParameterGroupConfig {

  @Parameter
  @Optional
  private ZonedDateTime zonedDateTime;

  @ParameterGroup(name = "parameter-group")
  SomeParameterGroupOneRequiredConfig someParameterGroup;

  public SomeParameterGroupOneRequiredConfig getSomeParameterGroup() {
    return someParameterGroup;
  }

  public ZonedDateTime getZonedDateTime() {
    return zonedDateTime;
  }
}
