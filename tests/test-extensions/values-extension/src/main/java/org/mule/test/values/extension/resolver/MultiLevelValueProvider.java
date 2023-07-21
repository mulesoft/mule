/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueBuilder;
import org.mule.runtime.extension.api.values.ValueProvider;

import java.util.Collections;
import java.util.Set;

public class MultiLevelValueProvider implements ValueProvider {

  public static final String AMERICA = "America";
  public static final String ARGENTINA = "Argentina";
  public static final String BUENOS_AIRES = "Buenos Aires";
  public static final String LA_PLATA = "La Plata";
  public static final String USA = "USA";
  public static final String USA_DISPLAY_NAME = "United States Of America";
  public static final String SAN_FRANCISCO = "San Francisco";

  @Override
  public Set<Value> resolve() {
    return Collections.singleton(ValueBuilder.newValue(AMERICA).withDisplayName(AMERICA)
        .withChild(ValueBuilder.newValue(ARGENTINA).withChild(ValueBuilder.newValue(LA_PLATA))
            .withChild(ValueBuilder.newValue(BUENOS_AIRES)))
        .withChild(ValueBuilder.newValue(USA).withDisplayName(USA_DISPLAY_NAME).withChild(ValueBuilder.newValue(SAN_FRANCISCO)))
        .build());
  }
}
