/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.implicit.config.extension.extension.internal;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.ConfigOverride;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.implicit.config.extension.extension.api.ImplicitConfigExtension;

public class ImplicitStatefulOperation {

  @Parameter
  @ConfigOverride
  private String optionalWithDefault;

  @MediaType("text/plain")
  public String getEnrichedName(@Config ImplicitConfigExtension config) {
    return config.getName() + " " + optionalWithDefault;
  }

}
