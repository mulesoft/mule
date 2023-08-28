/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.descriptor;

import org.mule.api.annotation.NoInstantiate;

/**
 * Thrown to indicate that a loader was not found in the repository
 */
@NoInstantiate
public final class LoaderNotFoundException extends Exception {

  /**
   * {@inheritDoc}
   */
  public LoaderNotFoundException(String message) {
    super(message);
  }
}
