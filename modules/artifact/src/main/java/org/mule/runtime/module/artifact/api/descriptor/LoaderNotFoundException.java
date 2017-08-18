/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.descriptor;

/**
 * Thrown to indicate that a loader was not found in the repository
 */
public class LoaderNotFoundException extends Exception {

  /**
   * {@inheritDoc}
   */
  public LoaderNotFoundException(String message) {
    super(message);
  }
}
