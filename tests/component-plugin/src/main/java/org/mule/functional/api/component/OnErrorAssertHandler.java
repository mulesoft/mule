/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.exception.AbstractExceptionListener;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.util.StringUtils;

import java.util.List;

public class OnErrorAssertHandler extends AbstractExceptionListener implements MessagingExceptionHandlerAcceptor {


  private List<LogChecker> checkers;

  @Override
  protected void doInitialise(MuleContext muleContext) throws InitialisationException {
    super.doInitialise(muleContext);
  }

  @Override
  public BaseEvent handleException(MessagingException exception, BaseEvent event) {
    exception.setHandled(true);
    String messageToLog = createMessageToLog(exception);
    StringBuilder errors = new StringBuilder();
    for (LogChecker checker : this.checkers) {
      try {
        checker.check(messageToLog);
      }catch(Exception e) {
        errors.append(e.getMessage());
      }
    }
    String errorMessage = errors.toString();
    if(!StringUtils.isBlank(errorMessage)) {
      throw new AssertionError(errorMessage);
    }
    return null;
  }

  @Override
  public boolean accept(BaseEvent event) {
    return true;
  }

  @Override
  public boolean acceptsAll() {
    return true;
  }

  public void setCheckers(List<LogChecker> logCheckers) {
    this.checkers = logCheckers;
  }

  public List<LogChecker> getCheckers() {
    return this.checkers;
  }

}
