/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import org.mule.runtime.api.value.Value;

import java.util.HashSet;
import java.util.Set;

/**
 * Adapter of the legacy {@link Value} to {@link org.mule.sdk.api.values.Value}
 *
 * @since 4.4.0
 */
public class SdkValueAdapter implements Value {

  private org.mule.sdk.api.values.Value value;

  public SdkValueAdapter(org.mule.sdk.api.values.Value value) {
    this.value = value;
  }

  @Override
  public String getId() {
    return value.getId();
  }

  @Override
  public String getDisplayName() {
    return value.getDisplayName();
  }

  @Override
  public Set<Value> getChilds() {
    Set<Value> values = new HashSet<>();
    value.getChilds().forEach(v -> values.add(new SdkValueAdapter(v)));
    return values;
  }

  @Override
  public String getPartName() {
    return value.getPartName();
  }
}
