/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
