/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;


import org.mule.api.annotation.NoInstantiate;

/**
 * Thrown to indicate that is not possible to load an object from the provided descriptor configuration.
 */
@NoInstantiate
public final class InvalidDescriptorLoaderException extends Exception {

  /**
   * {@inheritDoc}
   */
  public InvalidDescriptorLoaderException(String message) {
    super(message);
  }

  /**
   * {@inheritDoc}
   */
  public InvalidDescriptorLoaderException(String message, Throwable cause) {
    super(message, cause);
  }
}
