/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.param.Optional;

import java.util.List;
import java.util.Map;

public class PetCage {

  @Parameter
  @Optional
  Map<String, Integer> birds;

  @Parameter
  @Optional
  List<String> ammenities;

  public Map<String, Integer> getBirds() {
    return birds;
  }

  public List<String> getAmmenities() {
    return ammenities;
  }
}
