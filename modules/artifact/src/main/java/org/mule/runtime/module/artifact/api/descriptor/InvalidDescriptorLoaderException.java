/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;


/**
 * Thrown to indicate that is not possible to load an object from the provided descriptor configuration.
 */
public class InvalidDescriptorLoaderException extends Exception {

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
