/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;

import java.util.Collections;
import java.util.Set;

public class MultiLevelValueProvider implements ValueProvider {

  private static final String AMERICA = "America";
  private static final String ARGENTINA = "Argentina";
  private static final String BUENOS_AIRES = "Buenos Aires";
  private static final String LA_PLATA = "La Plata";
  private static final String USA = "USA";
  private static final String USA_DISPLAY_NAME = "United States Of America";
  private static final String SAN_FRANCISCO = "San Francisco";

  @Override
  public Set<Value> resolve() {
    return Collections.singleton(ValueBuilder.newValue(AMERICA).withDisplayName(AMERICA)
        .withChild(ValueBuilder.newValue(ARGENTINA).withChild(ValueBuilder.newValue(BUENOS_AIRES))
            .withChild(ValueBuilder.newValue(LA_PLATA)))
        .withChild(ValueBuilder.newValue(USA).withDisplayName(USA_DISPLAY_NAME).withChild(ValueBuilder.newValue(SAN_FRANCISCO)))
        .build());
  }
}
