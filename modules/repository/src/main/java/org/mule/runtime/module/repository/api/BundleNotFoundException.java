/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
