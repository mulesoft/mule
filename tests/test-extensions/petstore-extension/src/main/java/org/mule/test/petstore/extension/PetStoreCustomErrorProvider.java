/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.petstore.extension;

import static org.mule.test.petstore.extension.PetstoreErrorTypeDefinition.PET_ERROR;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Collections;
import java.util.Set;

public class PetStoreCustomErrorProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return Collections.singleton(PET_ERROR);
  }
}
