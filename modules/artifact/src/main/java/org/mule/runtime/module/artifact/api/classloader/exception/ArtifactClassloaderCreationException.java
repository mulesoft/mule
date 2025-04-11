/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader.exception;

/**
 * Exception used to wrap the cause of a classloader creation error, with a message indicating for which artifact the error
 * occurred.
 *
 * @since 4.9
 */
public final class ArtifactClassloaderCreationException extends Exception {

  private static final long serialVersionUID = 1L;

  public ArtifactClassloaderCreationException(String message, Throwable cause) {
    super(message, cause);
  }

}
