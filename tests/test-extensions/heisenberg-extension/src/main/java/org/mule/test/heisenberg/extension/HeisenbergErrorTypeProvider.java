/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension;



import org.mule.sdk.api.annotation.error.ErrorTypeProvider;
import org.mule.sdk.api.error.ErrorTypeDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class HeisenbergErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder()
        .add(HeisenbergErrors.HEALTH)
        .add(HeisenbergErrors.OAUTH2)
        .build();
  }
}
