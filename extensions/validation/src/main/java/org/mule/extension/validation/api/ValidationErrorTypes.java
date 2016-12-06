/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;


import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

/**
 * List of {@link ErrorTypeDefinition} that throws the {@link ValidationExtension}
 *
 * @since 4.0
 */
public enum ValidationErrorTypes implements ErrorTypeDefinition<ValidationErrorTypes> {

  /**
   * Indicates that a validation failure occurred
   */
  VALIDATION
}
