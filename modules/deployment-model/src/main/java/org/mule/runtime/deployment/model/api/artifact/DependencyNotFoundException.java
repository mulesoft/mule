/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.artifact;

/**
 * Exception thrown when a bundle could not be found in the repository.
 * // TODO(fernandezlautaro): MULE-10440 this class should be replaced with org.mule.runtime.module.repository.api.BundleNotFoundException
 * @since 4.0
 */
public class DependencyNotFoundException extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public DependencyNotFoundException(String message) {
    super(message);
  }
}
