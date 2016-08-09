/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.repository.api;

/**
 * Exception thrown when the repository system is not enabled.
 *
 * @since 4.0
 */
public class RepositoryServiceDisabledException extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public RepositoryServiceDisabledException(String message) {
    super(message);
  }

}
