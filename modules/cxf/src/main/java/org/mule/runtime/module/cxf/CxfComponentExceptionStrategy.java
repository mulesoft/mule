/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.exception.DefaultMessagingExceptionStrategy;

import org.apache.cxf.interceptor.Fault;

/**
 * This exception strategy forces the exception thrown from a web service invocation to be passed as-is, not wrapped in a Mule
 * exception object. This ensures the Cxf serialiser/deserialiser can send the correct exception object to the client.
 *
 * @deprecated Currently the result is the same if no exception strategy is defined within the flow. The only difference is that
 *             when you set the CxfComponentExceptionStrategy the exception is unwrapped inside of the exception block, but the
 *             exceptionPayload doesn't change.
 */
@Deprecated
public class CxfComponentExceptionStrategy extends DefaultMessagingExceptionStrategy {

  @Override
  protected MuleEvent doHandleException(Exception e, MuleEvent event) {
    if (e.getCause() instanceof Fault) {
      return super.doHandleException((Exception) e.getCause(), event);
    } else {
      return super.doHandleException(e, event);
    }
  }
}
