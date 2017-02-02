/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

import static java.util.Collections.singleton;
import static org.mule.extension.validation.api.ValidationErrorTypes.VALIDATION;

import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import java.util.Set;

/**
 * {@link ErrorTypeDefinition} for {@link ValidationExtension} operations.
 *
 * @since 4.0
 */
public class ValidationErrorTypeProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return singleton(VALIDATION);
  }
}
