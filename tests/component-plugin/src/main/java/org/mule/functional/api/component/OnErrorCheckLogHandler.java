/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static org.junit.Assert.fail;
import static org.mule.runtime.core.privileged.exception.MessagingExceptionUtils.markAsHandled;
import static org.mule.tck.processor.FlowAssert.addAssertion;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.privileged.exception.AbstractExceptionListener;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.tck.processor.FlowAssertion;

import java.util.List;

public class OnErrorCheckLogHandler extends AbstractExceptionListener
    implements MessagingExceptionHandlerAcceptor, FlowAssertion {


  private List<LogChecker> checkers;
  private StringBuilder errors;

  @Override
  public void start() throws MuleException {
    super.start();
    errors = new StringBuilder();
    addAssertion(getRootContainerName(), this);
  }

  @Override
  public CoreEvent handleException(Exception exception, CoreEvent event) {
    markAsHandled(exception);
    String messageToLog = createMessageToLog(exception);
    for (LogChecker checker : this.checkers) {
      try {
        checker.check(messageToLog);
      } catch (AssertionError e) {
        errors.append(e.getMessage());
      }
    }
    return null;
  }

  @Override
  public void verify() {
    String errorMessage = errors.toString();
    if (!StringUtils.isBlank(errorMessage)) {
      fail(errorMessage);
    }
  }

  @Override
  public boolean accept(CoreEvent event) {
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
