/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;

import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.reactivestreams.Publisher;

public class GlobalErrorHandler extends ErrorHandler {

  private boolean disposed;

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    throw new IllegalStateException("GlobalErrorHandlers should be used only as template for local ErrorHandlers");
  }

  @Override
  public void initialise() throws InitialisationException {
    setFromGlobalErrorHandler();
    super.initialise();
  }

  private void setFromGlobalErrorHandler() {
    this.getExceptionListeners().stream()
        .filter(exceptionListener -> exceptionListener instanceof TemplateOnErrorHandler)
        .forEach(exceptionListener -> ((TemplateOnErrorHandler) exceptionListener).setFromGlobalErrorHandler(true));
  }

  @Override
  public void stop() throws MuleException {}

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }
    try {
      super.stop();
      super.dispose();
      disposed = true;
    } catch (MuleException e) {
      logger.error("Could not stop global error handler.", e);
    }
  }
}
