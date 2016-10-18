/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

@ExclusiveOptionals(isOneRequired = true)
public class ExclusivePetBreeder {

  @Optional
  @Parameter
  private String mammals;

  @Optional
  @Parameter
  private String birds;

  public String getBirds() {
    return birds;
  }

  public String getMammals() {
    return mammals;
  }
}
