/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
