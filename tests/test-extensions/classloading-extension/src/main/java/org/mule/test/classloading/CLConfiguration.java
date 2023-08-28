/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
