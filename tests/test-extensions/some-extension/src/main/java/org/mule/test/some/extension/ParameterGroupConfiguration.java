/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
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

@Configuration(name = "pg-config")
@Operations({OtherOps.class})
public class ParameterGroupConfiguration {

  @Parameter
  @Optional
  ComplexParameter complexParameter;

  @Parameter
  @Optional
  String repeatedNameParameter;

  @ParameterGroup(name = "other-parameter-group")
  SomeParameterGroupOneRequiredConfig someParameterGroup;

  public SomeParameterGroupOneRequiredConfig getSomeParameterGroup() {
    return someParameterGroup;
  }

  public ComplexParameter getComplexParameter() {
    return complexParameter;
  }

  public String getRepeatedNameParameter() {
    return repeatedNameParameter;
  }
}
