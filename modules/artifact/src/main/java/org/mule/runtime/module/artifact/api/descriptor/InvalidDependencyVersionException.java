/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import org.mule.api.annotation.NoInstantiate;

/**
 * Thrown to indicate that a given bundle dependency version is not well formed.
 */
@NoInstantiate
public final class InvalidDependencyVersionException extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public InvalidDependencyVersionException(String message, Throwable cause) {
    super(message, cause);
  }
}
