/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.registry;

/**
 * Thrown to indicate that an object has an illegal dependency injection reference against another object
 */
public final class IllegalDependencyInjectionException extends RuntimeException {

  public IllegalDependencyInjectionException(String message) {
    super(message);
  }
}
