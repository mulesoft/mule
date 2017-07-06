/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
  @ValuePart(order = 2)
  @Parameter
  private String country;

  @Optional(defaultValue = "SFO")
  @DisplayName("State | City")
  @ValuePart(order = 3)
  @Parameter
  private String city;

}
