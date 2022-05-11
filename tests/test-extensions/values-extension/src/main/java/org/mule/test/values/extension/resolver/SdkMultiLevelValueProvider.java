/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;

import java.util.Collections;
import java.util.Set;

public class SdkMultiLevelValueProvider implements ValueProvider {

  public static final String AMERICA = "America";
  public static final String ARGENTINA = "Argentina";
  public static final String BUENOS_AIRES = "Buenos Aires";
  public static final String LA_PLATA = "La Plata";
  public static final String USA = "USA";
  public static final String USA_DISPLAY_NAME = "United States Of America";
  public static final String SAN_FRANCISCO = "San Francisco";

  @Override
  public Set<Value> resolve() {
    return Collections.singleton(
                                 ValueBuilder.newValue(AMERICA).withDisplayName(AMERICA)
                                     .withChild(ValueBuilder.newValue(ARGENTINA)
                                         .withChild(ValueBuilder.newValue(LA_PLATA))
                                         .withChild(ValueBuilder.newValue(BUENOS_AIRES)))
                                     .withChild(ValueBuilder.newValue(USA).withDisplayName(USA_DISPLAY_NAME)
                                         .withChild(ValueBuilder.newValue(SAN_FRANCISCO)))
                                     .build());
  }

  @Override
  public String getId() {
    return "SdkMultiLevelValueProvider-id";
  }
}
