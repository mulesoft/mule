/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
