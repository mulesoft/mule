/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.repository.api;

/**
 * Exception thrown when the repository system is not enabled.
 *
 * @since 4.0
 */
public final class RepositoryServiceDisabledException extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public RepositoryServiceDisabledException(String message) {
    super(message);
  }

}
