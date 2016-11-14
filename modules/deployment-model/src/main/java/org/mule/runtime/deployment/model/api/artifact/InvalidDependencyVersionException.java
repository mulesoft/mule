/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api.artifact;

/**
 * Thrown to indicate that a given bundle dependency version is not well formed.
 */
public class InvalidDependencyVersionException extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public InvalidDependencyVersionException(String message) {
    super(message);
  }
}
