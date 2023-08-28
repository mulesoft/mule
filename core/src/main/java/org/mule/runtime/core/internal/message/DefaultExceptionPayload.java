/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.message;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.ExceptionPayload;
import org.mule.runtime.core.internal.config.ExceptionHelper;

import java.util.Map;

@Deprecated
public class DefaultExceptionPayload implements ExceptionPayload {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -7114836033686599024L;

  private String message = null;
  private Map info = null;
  private Throwable exception;

  public DefaultExceptionPayload(Throwable exception) {
    this.exception = exception;
    MuleException muleRoot = ExceptionHelper.getRootMuleException(exception);
    if (muleRoot != null) {
      message = muleRoot.getMessage();
      info = muleRoot.getInfo();
    } else {
      message = exception.getMessage();
    }
  }

  @Override
  public Throwable getRootException() {
    return ExceptionHelper.getRootException(exception);
  }

  @Override
  public String getMessage() {
    return message;
  }

  @Override
  public Map getInfo() {
    return info;
  }

  @Override
  public Throwable getException() {
    return exception;
  }

}
