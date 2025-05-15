/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import static org.mule.tck.processor.FlowAssert.addAssertion;

import static java.util.Collections.singletonList;

import static org.junit.Assert.fail;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.util.StringUtils;
import org.mule.runtime.core.privileged.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.tck.processor.FlowAssertion;

import java.util.ArrayList;
import java.util.List;

import org.reactivestreams.Publisher;

import reactor.core.publisher.Flux;

public class OnErrorCheckLogHandler extends TemplateOnErrorHandler
    implements MessagingExceptionHandlerAcceptor, FlowAssertion {

  private final List<LogChecker> checkers = new ArrayList<>();
  private StringBuilder errors;
  private boolean propagate = false;
  private boolean succeedIfNoLog = false;

  // Flag to check if doLogException() was actually called. Since all the logic of the checkers is executed in that method,
  // if it's not called, we will never fail, even if we should've.
  private boolean exceptionLogged = false;

  // Flag to check if there was actually and exception handled.
  private boolean handledException = false;

  @Override
  protected void doInitialise() throws InitialisationException {
    // Add a dummy processor to force the routing logic into the execution chain
    setMessageProcessors(singletonList(event -> event));
    super.doInitialise();
  }

  @Override
  public void start() throws MuleException {
    super.start();
    errors = new StringBuilder();
    addAssertion(getRootContainerLocation().toString(), this);
    setHandleException(!propagate);
  }

  @Override
  protected Publisher<CoreEvent> route(Publisher<CoreEvent> eventPublisher) {
    return super.route(Flux.from(eventPublisher).doOnNext(e -> handledException = true));
  }

  /**
   * {@inheritDoc}
   *
   * @param buildFor
   */
  @Override
  public TemplateOnErrorHandler duplicateFor(ComponentLocation buildFor) {
    OnErrorCheckLogHandler cpy = new OnErrorCheckLogHandler();
    cpy.setFlowLocation(buildFor);
    cpy.propagate = this.propagate;
    cpy.succeedIfNoLog = this.succeedIfNoLog;
    cpy.exceptionLogged = this.exceptionLogged;
    cpy.handledException = this.handledException;
    when.ifPresent(expr -> cpy.setWhen(expr));
    cpy.setHandleException(this.handleException);
    cpy.setErrorType(this.errorType);
    cpy.setMessageProcessors(this.getMessageProcessors());
    cpy.setExceptionListener(this.getExceptionListener());
    cpy.setAnnotations(this.getAnnotations());
    return cpy;
  }

  @Override
  protected boolean logException(Throwable t, CoreEvent event) {
    Pair<MuleException, String> resolvedException = getExceptionListener().resolveExceptionAndMessageToLog(t);
    for (LogChecker checker : this.checkers) {
      try {
        checker.check(resolvedException.getSecond());
      } catch (AssertionError e) {
        errors.append(e.getMessage());
      }
    }

    if (super.logException(t, event)) {
      exceptionLogged = true;
      return true;
    } else {
      return false;
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
