/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.marvel.drstrange;

import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.runtime.extension.api.exception.ModuleException;

public class CustomErrorException extends ModuleException {

  public <T extends Enum<DrStrangeErrorTypeDefinition>> CustomErrorException(Throwable t,
                                                                             ErrorTypeDefinition<DrStrangeErrorTypeDefinition> errorTypeDefinition) {
    super(errorTypeDefinition, t);
  }
}
