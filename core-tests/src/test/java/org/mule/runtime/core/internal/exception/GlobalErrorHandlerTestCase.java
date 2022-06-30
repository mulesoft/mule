/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.util.MuleSystemProperties.REUSE_GLOBAL_ERROR_HANDLER_PROPERTY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.tck.util.MuleContextUtils.mockContextWithServices;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.GLOBAL_ERROR_HANDLER;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import static java.util.Arrays.asList;
import static java.util.Optional.empty;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.exception.ErrorHandlerTestCase.DefaultMessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@SmallTest
@Issue("W-11117613")
@Feature(ERROR_HANDLING)
@Story(GLOBAL_ERROR_HANDLER)
public class GlobalErrorHandlerTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemProperty reuseGlobalErrorHandler = new SystemProperty(REUSE_GLOBAL_ERROR_HANDLER_PROPERTY, "true");

  private GlobalErrorHandler globalErrorHandler;

  private final TemplateOnErrorHandler onErrorHandler = mock(TemplateOnErrorHandler.class);

  private final DefaultMessagingExceptionHandlerAcceptor defaultMessagingExceptionHandler =
      spy(new DefaultMessagingExceptionHandlerAcceptor());

  private final MuleContextWithRegistry mockMuleContext = mockContextWithServices();

  @Before
  public void setUp() throws Exception {
    globalErrorHandler = new GlobalErrorHandler();
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
  public void globalErrorHandlerDisposeOnce() throws InitialisationException {
    initialiseIfNeeded(globalErrorHandler, mockMuleContext);
    disposeIfNeeded(globalErrorHandler, null);
    disposeIfNeeded(globalErrorHandler, null);

    verify(onErrorHandler, times(1)).dispose();
  }

}
