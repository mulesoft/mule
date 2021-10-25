/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Collections.singletonList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.exception.MuleExceptionInfo;
import org.mule.runtime.core.api.exception.ErrorTypeMatcher;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.construct.FlowBackPressureException;
import org.mule.runtime.core.privileged.exception.AbstractDeclaredExceptionListener;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import org.junit.Test;

import io.qameta.allure.Issue;

public class ExceptionListenerTestCase extends AbstractMuleTestCase {

  @Test
  public void setSingleGoodProcessorEndpoint() throws Exception {
    AbstractDeclaredExceptionListener router = new OnErrorPropagateHandler();
    Processor messageProcessor = mock(Processor.class);
    router.setMessageProcessors(singletonList(messageProcessor));
    assertNotNull(router.getMessageProcessors());
    assertTrue(router.getMessageProcessors().contains(messageProcessor));
  }

  @Test
  public void setGoodProcessors() throws Exception {
    List<Processor> list = new ArrayList<>();
    list.add(mock(Processor.class));
    list.add(mock(Processor.class));

    AbstractDeclaredExceptionListener router = new OnErrorPropagateHandler();
    assertNotNull(router.getMessageProcessors());
    assertEquals(0, router.getMessageProcessors().size());

    router.setMessageProcessors(singletonList(mock(Processor.class)));
    assertEquals(1, router.getMessageProcessors().size());

    router.setMessageProcessors(list);
    assertNotNull(router.getMessageProcessors());
    assertEquals(2, router.getMessageProcessors().size());
  }

  @Test
  @Issue("MULE-19344")
  public void alwaysLogFlowBackPressureExceptions() throws Exception {
    OnCriticalErrorHandler handler = spy(new OnCriticalErrorHandler(mock(ErrorTypeMatcher.class)));
    Logger logger = mock(Logger.class);
    handler.getExceptionListener().setLogger(logger);
    FlowBackPressureException flowBackPressureException = mock(FlowBackPressureException.class);
    MuleExceptionInfo muleExceptionInfo = new MuleExceptionInfo();
    when(flowBackPressureException.getDetailedMessage()).thenReturn("Detail");
    when(flowBackPressureException.getExceptionInfo()).thenReturn(muleExceptionInfo);

    handler.logException(flowBackPressureException);
    assertTrue(muleExceptionInfo.isAlreadyLogged());
    handler.logException(flowBackPressureException);
    verify(logger, times(2)).error(anyString());
  }

}
