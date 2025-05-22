/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.exception;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.alert.AlertingSupport;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.event.EventContext;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleExceptionInfo;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.ast.internal.error.DefaultErrorTypeBuilder;
import org.mule.runtime.ast.privileged.error.DefaultErrorType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.exception.AbstractDeclaredExceptionListener;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.core.privileged.exception.TemplateOnErrorHandler;
import org.mule.runtime.core.privileged.message.PrivilegedError;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.VerboseExceptions;

import java.util.Collection;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Issue;

@RunWith(Parameterized.class)
public abstract class AbstractErrorHandlerTestCase extends AbstractMuleContextTestCase {

  @Rule
  public VerboseExceptions verbose;

  protected AlertingSupport alertingSupport;

  public AbstractErrorHandlerTestCase(VerboseExceptions verbose) {
    this.verbose = verbose;
  }

  @Parameters(name = "{0}")
  public static Collection<Object> data() {
    return asList(new VerboseExceptions(true),
                  new VerboseExceptions(false));
  }

  protected MessagingException mockException = mock(MessagingException.class);

  protected Flow flow;

  protected EventContext context;

  protected CoreEvent muleEvent;

  protected abstract AbstractDeclaredExceptionListener getErrorHandler();

  @Before
  public void before() throws Exception {
    flow = getTestFlow(muleContext);

    context = create(flow, TEST_CONNECTOR_LOCATION);
    muleEvent = CoreEvent.builder(context).message(of("")).build();

    when(mockException.getExceptionInfo()).thenReturn(new MuleExceptionInfo());

    alertingSupport = mock(AlertingSupport.class);
  }

  @Test
  @Issue("MULE-18562")
  public void testUnsuppressedErrorMustBeAccepted() throws InitialisationException {
    TemplateOnErrorHandler onErrorHandler = (TemplateOnErrorHandler) getErrorHandler();
    CoreEvent event = getCoreEventWithSuppressedError(onErrorHandler.getMuleContext());
    onErrorHandler.setErrorType("MULE:UNSUPPRESSED");
    initialiseIfNeeded(onErrorHandler, onErrorHandler.getMuleContext());
    assertThat(onErrorHandler.accept(event), is(true));
  }

  @Test
  @Issue("MULE-18562")
  public void testSuppressedErrorMustBeAccepted() throws InitialisationException {
    TemplateOnErrorHandler onErrorHandler = (TemplateOnErrorHandler) getErrorHandler();
    CoreEvent event = getCoreEventWithSuppressedError(onErrorHandler.getMuleContext());
    onErrorHandler.setErrorType("MULE:SUPPRESSED");
    initialiseIfNeeded(onErrorHandler, onErrorHandler.getMuleContext());
    assertThat(onErrorHandler.accept(event), is(true));
  }

  private CoreEvent getCoreEventWithSuppressedError(MuleContext muleContext) {
    ErrorType anyErrorType = DefaultErrorTypeBuilder.builder().namespace("MULE").identifier("ANY").build();
    ErrorType suppressedErrorType =
        DefaultErrorTypeBuilder.builder().namespace("MULE").identifier("SUPPRESSED").parentErrorType(anyErrorType).build();
    ErrorType unsuppressedErrorType =
        DefaultErrorTypeBuilder.builder().namespace("MULE").identifier("UNSUPPRESSED").parentErrorType(anyErrorType).build();
    ErrorType sourceResponseErrorType =
        DefaultErrorTypeBuilder.builder().namespace("MULE").identifier("SOURCE_RESPONSE").parentErrorType(anyErrorType).build();
    Error errorWithSuppression = mock(PrivilegedError.class);
    Error suppressedError = mock(Error.class);
    CoreEvent event = mock(CoreEvent.class);
    when(((PrivilegedError) errorWithSuppression).getSuppressedErrors()).thenReturn(singletonList(suppressedError));
    when(errorWithSuppression.getErrorType()).thenReturn(unsuppressedErrorType);
    when(suppressedError.getErrorType()).thenReturn(suppressedErrorType);
    when(event.getError()).thenReturn(Optional.of(errorWithSuppression));

    final ErrorTypeRepository errorTypeRepository = mock(ErrorTypeRepository.class);
    ((ContributedErrorTypeRepository) muleContext.getErrorTypeRepository()).setDelegate(errorTypeRepository);

    when(errorTypeRepository
        .lookupErrorType(ComponentIdentifier.buildFromStringRepresentation("MULE:SUPPRESSED")))
            .thenReturn(Optional.of(suppressedErrorType));
    when(errorTypeRepository
        .lookupErrorType(ComponentIdentifier.buildFromStringRepresentation("MULE:UNSUPPRESSED")))
            .thenReturn(Optional.of(unsuppressedErrorType));
    when(errorTypeRepository
        .getSourceResponseErrorType()).thenReturn(sourceResponseErrorType);

    return event;
  }

  protected void addErrorType(ComponentIdentifier errorTypeIdentifier) {
    final MuleExceptionInfo exceptionInfo = mock(MuleExceptionInfo.class);
    when(exceptionInfo.getErrorType())
        .thenReturn(new DefaultErrorType(errorTypeIdentifier.getName(), errorTypeIdentifier.getNamespace(), null));
    when(mockException.getExceptionInfo()).thenReturn(exceptionInfo);
  }

}
