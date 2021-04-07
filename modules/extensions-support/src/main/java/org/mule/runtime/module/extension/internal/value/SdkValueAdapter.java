/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toCollection;

import org.mule.sdk.api.values.Value;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapter of {@link org.mule.runtime.api.value.Value} to {@link org.mule.sdk.api.values.Value}
 *
 * @since 4.4.0
 */
public class SdkValueAdapter implements Value {

  private final org.mule.runtime.api.value.Value value;

  public SdkValueAdapter(org.mule.runtime.api.value.Value value) {
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
    Set<Value> values = value.getChilds()
        .stream().map(SdkValueAdapter::new)
        .collect(toCollection(LinkedHashSet::new));
    return unmodifiableSet(values);
  }

  @Override
  public String getPartName() {
    return value.getPartName();
  }
}
