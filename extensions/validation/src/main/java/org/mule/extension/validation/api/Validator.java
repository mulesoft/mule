/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.validation.api;

/**
 * A component which performs a validation and expresses its result through a {@link ValidationResult} object.
 * <p/>
 * Thread safeness is not to be assumed over instances of this class since the validator could be stateful.
 *
 * @since 4.0
 */
public interface Validator {

  /**
   * Performs the validation and generates a {@link ValidationResult} back.
   *
   * @return a {@link ValidationResult}
   */
  ValidationResult validate();
}
