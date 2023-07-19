/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.internal.artifact;

import org.mule.runtime.api.exception.MuleRuntimeException;

public class ExecutorExceptionWrapper extends MuleRuntimeException {

  public ExecutorExceptionWrapper(Throwable cause) {
    super(cause);
  }

}
