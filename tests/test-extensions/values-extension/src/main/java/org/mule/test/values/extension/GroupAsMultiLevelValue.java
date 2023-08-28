/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.values.ValuePart;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;

public class GroupAsMultiLevelValue {

  @Parameter
  @Optional(defaultValue = "some value")
  @Alias("required")
  private String wsdlLocation;

  @Optional(defaultValue = "AMERICA")
  @ValuePart(order = 1)
  @Parameter
  private String continent;

  @Optional(defaultValue = "USA")
  @org.mule.sdk.api.annotation.values.ValuePart(order = 2)
  @Parameter
  private String country;

  @Optional(defaultValue = "SFO")
  @DisplayName("State | City")
  @ValuePart(order = 3)
  @Parameter
  private String city;

}
