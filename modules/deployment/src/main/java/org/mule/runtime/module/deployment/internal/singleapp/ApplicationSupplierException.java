/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.internal.singleapp;


/**
 * Error that occurred during the provision of the app.
 *
 * @since 4.6.0
 */
public class ApplicationSupplierException extends RuntimeException {

  public ApplicationSupplierException(Exception e) {
    super(e);
  }
}
