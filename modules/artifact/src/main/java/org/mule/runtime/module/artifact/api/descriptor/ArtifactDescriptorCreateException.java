/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

/**
 * Thrown to indicate any error related to errors in the structure of a plugin file or folder.
 */
public class ArtifactDescriptorCreateException extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public ArtifactDescriptorCreateException(String message) {
    super(message);
  }

  /**
   * {@inheritDoc}
   */
  public ArtifactDescriptorCreateException(String s, Throwable throwable) {
    super(s, throwable);
  }

  /**
   * {@inheritDoc}
   */
  public ArtifactDescriptorCreateException(Throwable cause) {
    super(cause);
  }
}
