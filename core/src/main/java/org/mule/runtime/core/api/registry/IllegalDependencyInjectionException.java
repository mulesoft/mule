/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
