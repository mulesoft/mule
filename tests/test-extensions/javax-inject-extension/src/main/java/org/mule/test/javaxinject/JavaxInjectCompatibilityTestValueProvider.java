/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.javaxinject;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.sdk.api.values.Value;
import org.mule.sdk.api.values.ValueBuilder;
import org.mule.sdk.api.values.ValueProvider;
import org.mule.sdk.api.values.ValueResolvingException;

import java.util.Set;

import javax.inject.Inject;

public class JavaxInjectCompatibilityTestValueProvider implements ValueProvider {

  @Inject
  private ArtifactEncoding encoding;

  public String getId() {
    return "id";
  }

  public Set<Value> resolve() throws ValueResolvingException {
    return ValueBuilder.getValuesFor(encoding.getDefaultEncoding().name());
  }
}
