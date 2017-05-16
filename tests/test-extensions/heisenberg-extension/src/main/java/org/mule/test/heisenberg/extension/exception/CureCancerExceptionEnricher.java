/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension.exception;

import static org.mule.test.heisenberg.extension.HeisenbergErrors.HEALTH;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.exception.ExceptionHandler;

public class CureCancerExceptionEnricher extends ExceptionHandler {

  @Override
  public ModuleException enrichException(Exception e) {
    return new ModuleException(HEALTH, new HeisenbergException(e.getMessage(), e));
  }
}
