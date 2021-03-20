/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.value;

import org.mule.runtime.api.value.Value;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;

import java.util.HashSet;
import java.util.Set;

/**
 * Adapter of the legacy {@link ValueProvider} to {@link org.mule.sdk.api.values.ValueProvider}
 *
 * @since 4.4.0
 */
public class SdkValueProviderAdapter implements ValueProvider {

  private final org.mule.sdk.api.values.ValueProvider valueProvider;

  public SdkValueProviderAdapter(org.mule.sdk.api.values.ValueProvider valueProvider) {
    this.valueProvider = valueProvider;
  }

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    try {
      Set<Value> values = new HashSet<>();
      valueProvider.resolve().forEach(v -> values.add(new SdkValueAdapter(v)));
      return values;
    } catch (org.mule.sdk.api.values.ValueResolvingException e) {
      throw new ValueResolvingException(e.getMessage(), e.getFailureCode(), e.getCause());
    }
  }

  @Override
  public String getId() {
    return valueProvider.getId();
  }
}
