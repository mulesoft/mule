/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension.resolver;

import static java.util.Collections.singleton;
import static org.mule.sdk.api.values.ValueBuilder.newValue;

import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.test.values.extension.source.SourceMustNotStart;

import java.util.Set;

public class TrueFalseValueProvider implements ValueProvider {

  @Override
  public Set<Value> resolve() throws ValueResolvingException {
    return singleton(newValue(SourceMustNotStart.isStarted ? "TRUE" : "FALSE").build());
  }

  @Override
  public String getId() {
    return "TrueFalseValueProvider-id";
  }
}
