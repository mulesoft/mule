/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.heisenberg.extension.exception;

import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;
import org.mule.sdk.api.exception.ModuleException;

public class CureCancerExceptionEnricher extends ExceptionHandler {

  @Override
  public ModuleException enrichException(Exception e) {
    return new ModuleException(HEALTH, new HeisenbergException(e.getMessage(), e));
  }
}
