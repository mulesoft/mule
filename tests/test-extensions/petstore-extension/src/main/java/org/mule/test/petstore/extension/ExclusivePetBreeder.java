/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@ExclusiveOptionals(isOneRequired = true)
public class ExclusivePetBreeder {

  @Optional
  @Parameter
  @Alias("mammals")
  private String unaliasedNammals;

  @Optional
  @Parameter
  private String birds;

  public String getBirds() {
    return birds;
  }

  public String getunaliasedNammals() {
    return unaliasedNammals;
  }
}
