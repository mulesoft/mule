/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.exception.ErrorHandlerTestCase.DefaultMessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mule.tck.junit4.AbstractMuleTestCase;


public class GlobalErrorHandlerTestCase extends AbstractMuleTestCase {

  private GlobalErrorHandler globalErrorHandler;

  private final TemplateOnErrorHandler onErrorHandler = mock(TemplateOnErrorHandler.class);

  private final DefaultMessagingExceptionHandlerAcceptor defaultMessagingExceptionHandler =
      spy(new DefaultMessagingExceptionHandlerAcceptor());

  private final MuleContextWithRegistry mockMuleContext = mockContextWithServices();

  @Before
  public void setUp() throws Exception {
    globalErrorHandler = new GlobalErrorHandler();
    globalErrorHandler.featureFlaggingService = getFeatureFlaggingService();
    when(onErrorHandler.isInitialised()).thenReturn(true);
    globalErrorHandler.setExceptionListeners(new ArrayList<>(asList(onErrorHandler)));
    when(mockMuleContext.getDefaultErrorHandler(empty())).thenReturn(defaultMessagingExceptionHandler);
    globalErrorHandler.setMuleContext(mockMuleContext);
    globalErrorHandler.setRootContainerName("root");
  }

  @Test
  public void globalErrorHandlerInitialiseOnce() throws Exception {
    initialiseIfNeeded(globalErrorHandler, mockMuleContext);
    initialiseIfNeeded(globalErrorHandler, mockMuleContext);

    verify(onErrorHandler, times(1)).setFromGlobalErrorHandler(true);
    verify(onErrorHandler, times(1)).initialise();
  }

  @Test
  public void globalErrorHandlerStartOnce() throws Exception {
    startIfNeeded(globalErrorHandler);
    startIfNeeded(globalErrorHandler);

    verify(onErrorHandler, times(1)).start();
  }

  @Test
  public void globalErrorHandlerStopOnce() throws Exception {
    startIfNeeded(globalErrorHandler);
    startIfNeeded(globalErrorHandler);
    stopIfNeeded(globalErrorHandler);
    stopIfNeeded(globalErrorHandler);

    verify(onErrorHandler, times(1)).stop();
  }

  @Test
  public void globalErrorHandlerDisposeOnce() {
    disposeIfNeeded(globalErrorHandler, null);
    disposeIfNeeded(globalErrorHandler, null);

    verify(onErrorHandler, times(1)).dispose();
  }

  @Test
  public void doNotSetFromGlobalErrorHandlerWhenFeatureFlagIsDisabled() throws InitialisationException {
    globalErrorHandler.featureFlaggingService = feature -> false;
    initialiseIfNeeded(globalErrorHandler, mockMuleContext);


    verify(onErrorHandler, times(0)).setFromGlobalErrorHandler(true);
    verify(onErrorHandler, times(1)).initialise();
  }
}
