/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.metadata;

/**
 * Represents a multilevel metadata key that describes a soap operation giving a web service.
 *
 * @since 1.0
 */
public final class WebServiceTypeKey {

  /**
   * The service that contains the operation
   */
  private String service;

  /**
   * The operation to be executed
   */
  private String operation;

  public String getService() {
    return service;
  }

  public String getOperation() {
    return operation;
  }
}
