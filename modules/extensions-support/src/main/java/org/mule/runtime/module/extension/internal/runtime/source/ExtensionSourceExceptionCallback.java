/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.execution.ExceptionCallback;
import org.mule.runtime.api.message.MuleEvent;
import org.mule.runtime.core.execution.ResponseCompletionCallback;

/**
 * Channels exceptions through the
 * {@link ResponseCompletionCallback#responseSentWithFailure(Exception, org.mule.runtime.core.api.MuleEvent)}.
 *
 * @since 4.0
 */
class ExtensionSourceExceptionCallback implements ExceptionCallback<MuleEvent, Exception> {

  private final ResponseCompletionCallback completionCallback;
  private final MuleEvent muleEvent;

  /**
   * Creates a new instance
   *
   * @param completionCallback the callback used to send the failure response
   * @param muleEvent the related {@link MuleEvent}
   */
  ExtensionSourceExceptionCallback(ResponseCompletionCallback completionCallback, MuleEvent muleEvent) {
    this.completionCallback = completionCallback;
    this.muleEvent = muleEvent;
  }

  /**
   * Invokes {@link ResponseCompletionCallback#responseSentWithFailure(Exception, org.mule.runtime.core.api.MuleEvent)} over the
   * {@link #completionCallback}, using the {@code exception} and {@link #muleEvent}
   *
   * @param exception a {@link Exception}
   * @return a response {@link MuleEvent}
   */
  @Override
  public MuleEvent onException(Exception exception) {
    return completionCallback.responseSentWithFailure(exception, (org.mule.runtime.core.api.MuleEvent) muleEvent);
  }
}
