/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.repository.api;

/**
 * Exception thrown when a bundle could not be found in the repository.
 *
 * @since 4.0
 */
public final class BundleNotFoundException extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public BundleNotFoundException(Throwable cause) {
    super(cause);
  }
}
