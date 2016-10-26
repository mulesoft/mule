/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.classloadermodel;

/**
 * Typed exception thrown when creating a ClassloaderModel if it's malformed.
 *
 * @since 4.0
 */
public class MalformedClassloaderModelException extends Exception {

  public MalformedClassloaderModelException(String message, Throwable cause) {
    super(message, cause);
  }

  public MalformedClassloaderModelException(String message) {
    super(message);
  }
}
