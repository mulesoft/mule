/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
