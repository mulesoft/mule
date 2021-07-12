/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
