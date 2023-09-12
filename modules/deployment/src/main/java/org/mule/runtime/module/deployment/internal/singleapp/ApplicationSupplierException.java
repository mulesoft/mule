/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
