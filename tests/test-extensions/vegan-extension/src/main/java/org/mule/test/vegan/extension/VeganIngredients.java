/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.vegan.extension;

import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@ExclusiveOptionals
public class VeganIngredients {

  @Parameter
  @Optional
  private int saltMiligrams;

  @Parameter
  @Optional
  private String saltReplacementName;

  public int getSaltMiligrams() {
    return saltMiligrams;
  }

  public String getSaltReplacementName() {
    return saltReplacementName;
  }
}
