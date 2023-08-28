/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.api.discoverer;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.service.Service;

/**
 * Thrown to indicate an error during the resolution process of {@link Service} instances.
 */
@NoInstantiate
public final class ServiceResolutionError extends Exception {

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
