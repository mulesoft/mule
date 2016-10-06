/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.el.v2;

import java.util.Optional;

/**
 * Represents the result of an expression validation.
 *
 * @since 4.0
 */
public interface ValidationResult {

  /**
   * @return an optional representing the validation error or an empty one
   */
  Optional<String> errorMessage();

  /**
   * @return true if the validation was ok, false otherwise
   */
  boolean success();

}
