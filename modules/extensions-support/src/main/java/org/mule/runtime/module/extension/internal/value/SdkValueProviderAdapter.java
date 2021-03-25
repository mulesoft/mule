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
import org.mule.sdk.api.values.ValueProvider;
import org.mule.sdk.api.values.ValueResolvingException;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapter of the legacy {@link org.mule.runtime.extension.api.values.ValueProvider} to {@link ValueProvider}
 *
 * @since 4.4.0
 */
public class SdkValueProviderAdapter implements ValueProvider {

  private final org.mule.runtime.extension.api.values.ValueProvider valueProvider;

  public SdkValueProviderAdapter(org.mule.runtime.extension.api.values.ValueProvider valueProvider) {
    this.valueProvider = valueProvider;
  }

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    try {
      Set<Value> values = valueProvider.resolve()
          .stream().map(SdkValueAdapter::new)
          .collect(toCollection(LinkedHashSet::new));
      return unmodifiableSet(values);
    } catch (org.mule.runtime.extension.api.values.ValueResolvingException e) {
      throw new ValueResolvingException(e.getMessage(), e.getFailureCode(), e.getCause());
    }
  }

  @Override
  public String getId() {
    return valueProvider.getId();
  }
}
