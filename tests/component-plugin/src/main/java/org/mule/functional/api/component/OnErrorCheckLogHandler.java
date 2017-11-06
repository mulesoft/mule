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
import java.util.function.Function;

import org.reactivestreams.Publisher;

public class OnErrorCheckLogHandler extends TemplateOnErrorHandler
    implements MessagingExceptionHandlerAcceptor, FlowAssertion {

  private List<LogChecker> checkers = new ArrayList<>();
  private StringBuilder errors;
  private boolean propagate = false;
  private boolean succeedIfNoLog = false;

  //Flag to check if doLogException() was actually called. Since all the logic of the checkers is executed in that method,
  //if it's not called, we will never fail, even if we should've.
  private boolean exceptionLogged = false;

  //Flag to check if there was actually and exception handled.
  private boolean handledException = false;

  @Override
  public void start() throws MuleException {
    super.start();
    errors = new StringBuilder();
    addAssertion(getRootContainerLocation().toString(), this);
    setHandleException(!propagate);
  }

  @Override
  protected Function<CoreEvent, Publisher<CoreEvent>> route(Exception exception) {
    handledException = true;
    return super.route(exception);
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
    if (!handledException) {
      fail("Handler could not check any exception log because no exception was raised");
    }
    String errorMessage = errors.toString();
    if (!StringUtils.isBlank(errorMessage)) {
      fail(errorMessage);
    }
    if (!exceptionLogged && !succeedIfNoLog) {
      fail("Could not check exception because it was never logged");
    }
  }

  @Override
  public boolean acceptsAll() {
    return true;
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

  public void setSucceedIfNoLog(boolean succeedIfNoLog) {
    this.succeedIfNoLog = succeedIfNoLog;
  }

}
