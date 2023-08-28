/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.value;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toCollection;

import org.mule.runtime.api.value.Value;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapter of {@link org.mule.sdk.api.values.Value} to {@link org.mule.runtime.api.value.Value}
 *
 * @since 4.4.0
 */
public class MuleValueAdapter implements Value {

  private final org.mule.sdk.api.values.Value value;

  public MuleValueAdapter(org.mule.sdk.api.values.Value value) {
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
    Set<Value> values = value.getChilds().stream()
        .map(MuleValueAdapter::new)
        .collect(toCollection(LinkedHashSet::new));
    return unmodifiableSet(values);
  }

  @Override
  public String getPartName() {
    return value.getPartName();
  }
}
