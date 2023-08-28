/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.error;

import static org.mule.runtime.config.internal.error.MuleCoreErrorTypeRepository.MULE_CORE_ERROR_TYPE_REPOSITORY;

import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.ast.api.error.ErrorTypeRepositoryProvider;

public class CoreErrorTypeRepositoryProvider implements ErrorTypeRepositoryProvider {

  @Override
  public ErrorTypeRepository get() {
    return MULE_CORE_ERROR_TYPE_REPOSITORY;
  }
}
