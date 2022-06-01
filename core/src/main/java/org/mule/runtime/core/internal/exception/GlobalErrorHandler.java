/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.util.MuleSystemProperties.REVERT_SIGLETON_ERROR_HANDLER_PROPERTY;

import static java.lang.Boolean.getBoolean;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;

import org.reactivestreams.Publisher;

public class GlobalErrorHandler extends ErrorHandler {

  private static final boolean IS_PROTOTYPE = getBoolean(REVERT_SIGLETON_ERROR_HANDLER_PROPERTY);

  // We need to keep a reference to one of the local error handlers to be able to stop its inner processors.
  // This is a temporary solution and won't be necessary after W-10674245.
  // TODO: W-10674245 remove this
  private ErrorHandler local;

  @Override
  public Publisher<CoreEvent> apply(Exception exception) {
    throw new IllegalStateException("GlobalErrorHandlers should be used only as template for local ErrorHandlers");
  }

  @Override
  public void stop() throws MuleException {
    if (!IS_PROTOTYPE) {
      ((LocalErrorHandler) local).stopParent();
    }
  }

  public ErrorHandler createLocalErrorHandler(Location flowLocation) {
    ErrorHandler local;
    if (IS_PROTOTYPE) {
      local = new ErrorHandler();
    } else {
      local = new LocalErrorHandler();
    }
    local.setName(this.name);
    local.setExceptionListeners(this.getExceptionListeners());
    local.setExceptionListenersLocation(flowLocation);
    if (this.local == null) {
      this.local = local;
    }
    return local;
  }
}
