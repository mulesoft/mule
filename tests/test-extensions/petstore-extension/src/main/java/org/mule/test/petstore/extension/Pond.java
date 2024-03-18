/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@ExclusiveOptionals(isOneRequired = true)
public class Pond {

  @Parameter
  @Optional
  public String frogName;

  @Parameter
  @Optional
  public String fishName;

  public String getFishName() {
    return fishName;
  }

  public String getFrogName() {
    return frogName;
  }

}
