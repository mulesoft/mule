/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.launcher.coreextension;

/**
 * Thrown to indicate that a mule core extension dependency was not successfully resolved.
 */
public class UnresolveableDependencyException extends RuntimeException {

  public UnresolveableDependencyException(String message) {
    super(message);
  }
}
