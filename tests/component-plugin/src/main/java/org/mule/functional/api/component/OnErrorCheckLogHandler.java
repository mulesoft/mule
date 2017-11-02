/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static org.junit.Assert.fail;
import static org.mule.tck.processor.FlowAssert.addAssertion;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.tck.processor.FlowAssertion;

import java.util.ArrayList;
import java.util.List;

public class OnErrorCheckLogHandler extends TemplateOnErrorHandler
    implements MessagingExceptionHandlerAcceptor, FlowAssertion {

  private List<LogChecker> checkers = new ArrayList<>();
  private StringBuilder errors;
  private boolean propagate = false;

  //Flag to check if doLogException() was actually called. Since all the logic of the checkers is executed in that method,
  //if it's not called, we will never fail, even if we should've.
  private boolean exceptionLogged = false;

  @Override
  public void start() throws MuleException {
    super.start();
    errors = new StringBuilder();
    addAssertion(getRootContainerLocation().toString(), this);
    setHandleException(!propagate);
  }

  @Override
  protected void doLogException(String message, Throwable t) {
    exceptionLogged = true;
    for (LogChecker checker : this.checkers) {
      try {
        checker.check(message);
      } catch (AssertionError e) {
        errors.append(e.getMessage());
      }
    }
  }

  @Override
  public void verify() {
    String errorMessage = errors.toString();
    if (!StringUtils.isBlank(errorMessage)) {
      fail(errorMessage);
    }
    if (!exceptionLogged) {
      fail("Could not check exception because it was never logged");
    }
  }

  @Override
  public boolean accept(CoreEvent event) {
    return true;
  }

  @Override
  public boolean acceptsAll() {
    return false;
  }

  public void setCheckers(List<LogChecker> logCheckers) {
    this.checkers.addAll(logCheckers);
  }

  public List<LogChecker> getCheckers() {
    return this.checkers;
  }

  public void setPropagate(boolean propagate) {
    this.propagate = propagate;
  }

}
