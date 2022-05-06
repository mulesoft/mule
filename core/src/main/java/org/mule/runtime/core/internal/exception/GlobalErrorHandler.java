/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.core.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.CoreEvent;

import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.reactivestreams.Publisher;

import javax.inject.Inject;

import static java.lang.Boolean.getBoolean;
import static org.mule.runtime.api.config.MuleRuntimeFeature.REUSE_GLOBAL_ERROR_HANDLER;
import static org.mule.runtime.api.util.MuleSystemProperties.REVERT_SIGLETON_ERROR_HANDLER_PROPERTY;

public class GlobalErrorHandler extends ErrorHandler {

  private static final boolean IS_PROTOTYPE = getBoolean(REVERT_SIGLETON_ERROR_HANDLER_PROPERTY);

  // We need to keep a reference to one of the local error handlers to be able to stop its inner processors.
  // This is needed for disabling the feature flag REUSE_GLOBAL_ERROR_HANDLER.
  private ErrorHandler local;

  private boolean disposed;

  @Inject
  private FeatureFlaggingService featureFlaggingService;

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

  @Override
  public void stop() throws MuleException {
    if (!featureFlaggingService.isEnabled(REUSE_GLOBAL_ERROR_HANDLER)) {
      if (!IS_PROTOTYPE) {
        ((LocalErrorHandler) local).stopParent();
      }
    }
  }

  @Override
  public void dispose() {
    if (!featureFlaggingService.isEnabled(REUSE_GLOBAL_ERROR_HANDLER)) {
      super.dispose();
      return;
    }
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
