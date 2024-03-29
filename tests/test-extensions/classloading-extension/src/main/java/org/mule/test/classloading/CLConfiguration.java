/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.test.classloading.internal.AllOptionalParameterGroup;

@Configuration(name = "config")
@Operations(CLOperations.class)
public class CLConfiguration {

  @ParameterGroup(name = "allOptional", showInDsl = true)
  private AllOptionalParameterGroup allOptionals;

  public AllOptionalParameterGroup getAllOptionals() {
    return allOptionals;
  }
}
