/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.junit4;

import org.mule.runtime.core.api.MuleContext;

/**
 * Exception thrown when the {@link MuleContext} was created successfully but a lifecycle phase failed.
 *
 * @since 4.0
 */
public class ContextLifecycleException extends RuntimeException {

  private MuleContext muleContext;

  public ContextLifecycleException(Exception e, MuleContext muleContext) {
    super(e);
    this.muleContext = muleContext;
  }

  public MuleContext getMuleContext() {
    return muleContext;
  }
}
