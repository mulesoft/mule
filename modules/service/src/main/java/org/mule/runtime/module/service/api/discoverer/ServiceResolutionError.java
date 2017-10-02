/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.api.discoverer;

import org.mule.runtime.api.service.Service;

/**
 * Thrown to indicate an error during the resolution process of {@link Service} instances.
 */
public class ServiceResolutionError extends Exception {

  /**
   * @inherited
   */
  public ServiceResolutionError(String message) {
    super(message);
  }

  /**
   * @inherited
   */
  public ServiceResolutionError(String message, Throwable cause) {
    super(message, cause);
  }
}
