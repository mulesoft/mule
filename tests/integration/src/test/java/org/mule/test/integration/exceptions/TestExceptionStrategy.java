/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.exception.AbstractMessagingExceptionStrategy;

public class TestExceptionStrategy extends AbstractMessagingExceptionStrategy {

  public TestExceptionStrategy() {
    super(null);
  }

  @Override
  public MuleEvent handleException(Exception exception, MuleEvent event) {
    MuleEvent result = super.handleException(exception, event);
    result.setMessage(MuleMessage.builder(result.getMessage()).payload("Ka-boom!").build());
    return result;
  }

  public boolean isRedeliver() {
    return false;
  }
}
