/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.internal.artifact.params;

public class ExpressionNotSupportedException extends RuntimeException {

  public ExpressionNotSupportedException(String message) {
    super(message);
  }

}
