/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.exception.Errors.ComponentIdentifiers.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.internal.execution.ModuleFlowProcessingPhase.ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.junit4.matcher.EitherMatcher.leftMatches;
import static org.mule.tck.junit4.matcher.EitherMatcher.rightMatches;
import static org.mule.tck.junit4.matcher.EventMatcher.hasErrorType;
import static org.mule.tck.junit4.matcher.MessagingExceptionMatcher.withEventThat;
import static reactor.core.publisher.Mono.just;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.DefaultMuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.execution.MessageProcessContext;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.message.ErrorBuilder;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;
import org.mule.runtime.core.policy.FailureSourcePolicyResult;
import org.mule.runtime.core.policy.MessageSourceResponseParametersProcessor;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.core.policy.SourcePolicy;
import org.mule.runtime.core.policy.SuccessSourcePolicyResult;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.EventMatcher;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@RunWith(Parameterized.class)
@SmallTest
public class ModuleFlowProcessingPhaseTestCase extends AbstractMuleTestCase {

  private static final ErrorType ERROR_FROM_FLOW =
      ErrorTypeBuilder.builder().parentErrorType(mock(ErrorType.class)).namespace("TEST").identifier("FLOW_FAILED").build();

  private boolean enableSourcePolicies;

  @Rule
  public SystemProperty enableSourcePoliciesSystemProperty;

  private MuleContext muleContext;
  private FlowConstruct flow;
  private MessageProcessContext context;
  private ModuleFlowProcessingPhaseTemplate template;
  private PhaseResultNotifier notifier;

  private SourcePolicy sourcePolicy;

  private ModuleFlowProcessingPhase moduleFlowProcessingPhase;

  // TODO MULE-11167 Remove this parameterization once policies sources are non-blocking
  public ModuleFlowProcessingPhaseTestCase(boolean enableSourcePolicies) {
    this.enableSourcePolicies = enableSourcePolicies;
    if (enableSourcePolicies) {
      this.enableSourcePoliciesSystemProperty = new SystemProperty(ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY, "true");
    } else {
      this.enableSourcePoliciesSystemProperty = new SystemProperty(ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY, null);
    }
  }

  @Parameters
  public static Collection<Object> data() {
    return asList(new Object[] {false, true});
  }

  @Before
  public void before() throws Exception {
    // Just load the class so the reactor hooks are registered.
    new DefaultMuleContext();

    muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    when(muleContext.getErrorTypeRepository()).thenReturn(createDefaultErrorTypeRepository());
    when(muleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));

    final PolicyManager policyManager = mock(PolicyManager.class);
    sourcePolicy = mock(SourcePolicy.class);
    when(policyManager.createSourcePolicyInstance(any(), any(), any(), any())).thenReturn(sourcePolicy);
    doAnswer(invocation -> Either
        .right(new SuccessSourcePolicyResult(invocation.getArgumentAt(0, Event.class), emptyMap(), null))).when(sourcePolicy)
            .process(any());

    moduleFlowProcessingPhase = new ModuleFlowProcessingPhase(policyManager);
    initialiseIfNeeded(moduleFlowProcessingPhase, muleContext);

    flow = mock(FlowConstruct.class);
    final MessagingExceptionHandler exceptionHandler = mock(MessagingExceptionHandler.class);
    when(flow.getExceptionListener()).thenReturn(exceptionHandler);
    when(flow.getMuleContext()).thenReturn(muleContext);

    context = mock(MessageProcessContext.class);
    when(context.getFlowConstruct()).thenReturn(flow);
    final MessageSource source = mock(MessageSource.class);
    when(source.getLocation()).thenReturn(fromSingleComponent("/0"));
    when(context.getMessageSource()).thenReturn(source);
    when(context.getTransactionConfig()).thenReturn(empty());

    template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));
    when(template.sendResponseToClient(any(), any(), any(), any())).thenAnswer(invocation -> {
      invocation.getArgumentAt(3, ResponseCompletionCallback.class).responseSentSuccessfully();
      return Mono.empty();
    });

    when(template.sendFailureResponseToClient(any(), any(), any())).thenAnswer(invocation -> {
      invocation.getArgumentAt(2, ResponseCompletionCallback.class).responseSentSuccessfully();
      return Mono.empty();
    });

    notifier = mock(PhaseResultNotifier.class);
  }

  @Test
  public void success() throws Exception {
    configureSuccessfulFlow(template);
    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());
    moduleFlowProcessingPhase.runPhase(template, context, notifier);
    verifySuccess();
  }

  private void verifySuccess() {
    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template).sendResponseToClient(any(), any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(rightMatches(Matchers.any(Event.class))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  @Test
  public void successWithAsyncResponse() throws Exception {
    assumeThat(enableSourcePolicies, is(false));
    Reference<MonoSink> sinkReference = new Reference<>();
    reset(template);
    when(template.sendResponseToClient(any(), any(), any(), any())).thenAnswer(invocation -> {
      invocation.getArgumentAt(3, ResponseCompletionCallback.class).responseSentSuccessfully();
      return Mono.create(sinkReference::set);
    });

    when(template.getMessage()).thenReturn(Message.of(null));

    configureSuccessfulFlow(template);
    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    moduleFlowProcessingPhase.runPhase(template, context, notifier);
    verify(template, never()).sendAfterTerminateResponseToClient(any());
    sinkReference.get().success();
    verifySuccess();
  }

  @Test
  public void sourceResponseGenerateErrorType() throws Exception {
    configureSuccessfulFlow(template);
    configureFailureResponse();

    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseGenerate());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    // The error handler is called with the apropriate type, but for the termination it counts as successful if the error response
    // was sent.
    verify(template).sendAfterTerminateResponseToClient(argThat(rightMatches(Matchers.any(Event.class))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  @Test
  public void sourceResponseSendErrorType() throws Exception {
    configureSuccessfulFlow(template);

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());
    configureFailingResponseSend();
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseSend());
    verify(template).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    // The error handler is called with the apropriate type, but for the termination it counts as successful if the error response
    // was sent.
    verify(template).sendAfterTerminateResponseToClient(argThat(rightMatches(Matchers.any(Event.class))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  @Test
  public void flowError() throws Exception {
    configureFailingFlow(template, mockException());

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowError();
  }

  private void verifyFlowError() {
    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(leftMatches(withEventThat(isErrorTypeFlowFailure()))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  @Test
  public void flowErrorWithAsyncResponse() throws Exception {
    assumeThat(enableSourcePolicies, is(false));
    Reference<MonoSink> sinkReference = new Reference<>();
    Reference<Exception> exception = new Reference<>();

    reset(template);
    when(template.getMessage()).thenReturn(Message.of(null));
    when(template.sendFailureResponseToClient(any(), any(), any())).thenAnswer(invocation -> {
      invocation.getArgumentAt(2, ResponseCompletionCallback.class).responseSentSuccessfully();
      Event event = invocation.getArgumentAt(0, MessagingException.class).getEvent();
      exception.set(buildFailingFlowException(event, mockException()));
      return Mono.create(sinkReference::set);
    });

    configureFailingFlow(template, mockException());

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(template, never()).sendAfterTerminateResponseToClient(any());

    sinkReference.get().error(exception.get());
    verifyFlowError();
  }

  @Test
  public void sourceErrorResponseGenerateErrorType() throws Exception {
    configureSuccessfulFlow(template);
    configureFailureResponse();

    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> {
      throw mockException();
    });

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseGenerate());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template)
        .sendAfterTerminateResponseToClient(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseGenerate()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException().getClass())));
  }

  @Test
  public void sourceErrorResponseSendErrorType() throws Exception {
    configureSuccessfulFlow(template);
    configureFailureResponse();

    configureFailingErrorSend();
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseGenerate());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template)
        .sendAfterTerminateResponseToClient(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseSend()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException().getClass())));
  }

  @Test
  public void sourceErrorResponseGenerateErrorTypeAfterFlowError() throws Exception {
    configureFailingFlow(template, mockException());

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> {
      fail("Must not call success handling methods");
      return emptyMap();
    });
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> {
      throw mockException();
    });

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template)
        .sendAfterTerminateResponseToClient(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseGenerate()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException().getClass())));
  }

  @Test
  public void sourceErrorResponseGenerateErrorTypeDuringHandler() throws Exception {
    configureErrorHandlingFailingFlow(template, mockException());
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template)
        .sendAfterTerminateResponseToClient(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseGenerate()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException().getClass())));
  }

  @Test
  public void sourceErrorResponseSendErrorTypeAfterFlowError() throws Exception {
    configureFailingFlow(template, mockException());

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> {
      fail("Must not call success handling methods");
      return emptyMap();
    });
    configureFailingErrorSend();
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template)
        .sendAfterTerminateResponseToClient(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseSend()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException().getClass())));
  }

  private void verifyFlowErrorHandler(final EventMatcher errorHandlerEventMatcher) {
    if (enableSourcePolicies) {
      // TODO MULE-11167 policy failures do not call the error handler?
      verify(flow.getExceptionListener(), never()).handleException(any(), any());
    } else {
      verify(flow.getExceptionListener()).handleException(any(), argThat(errorHandlerEventMatcher));
    }
  }

  private EventMatcher isErrorTypeSourceResponseGenerate() {
    return hasErrorType(SOURCE_RESPONSE_GENERATE.getNamespace(), SOURCE_RESPONSE_GENERATE.getName());
  }

  private EventMatcher isErrorTypeSourceResponseSend() {
    return hasErrorType(SOURCE_RESPONSE_SEND.getNamespace(), SOURCE_RESPONSE_SEND.getName());
  }

  private EventMatcher isErrorTypeFlowFailure() {
    return hasErrorType(ERROR_FROM_FLOW.getNamespace(), ERROR_FROM_FLOW.getIdentifier());
  }

  private EventMatcher isErrorTypeSourceErrorResponseGenerate() {
    return hasErrorType(SOURCE_ERROR_RESPONSE_GENERATE.getNamespace(), SOURCE_ERROR_RESPONSE_GENERATE.getName());
  }

  private EventMatcher isErrorTypeSourceErrorResponseSend() {
    return hasErrorType(SOURCE_ERROR_RESPONSE_SEND.getNamespace(), SOURCE_ERROR_RESPONSE_SEND.getName());
  }

  private void configureSuccessfulFlow(final ModuleFlowProcessingPhaseTemplate template) throws Exception {
    when(template.routeEventAsync(any())).then(invocation -> just(invocation.getArgumentAt(0, Event.class)));
    when(sourcePolicy.process(any())).thenAnswer(invocation -> {
      return right(new SuccessSourcePolicyResult(invocation.getArgumentAt(0, Event.class),
                                                 emptyMap(), mock(MessageSourceResponseParametersProcessor.class)));
    });
  }

  private void configureThrowingFlow(final ModuleFlowProcessingPhaseTemplate template, RuntimeException failure,
                                     boolean inErrorHandler)
      throws Exception {
    when(template.routeEventAsync(any())).then(invocation -> {
      MessagingException me = buildFailingFlowException(invocation.getArgumentAt(0, Event.class), failure);
      me.setInErrorHandler(inErrorHandler);
      throw me;
    });
    when(sourcePolicy.process(any())).thenAnswer(invocation -> {
      MessagingException me = buildFailingFlowException(invocation.getArgumentAt(0, Event.class), failure);
      me.setInErrorHandler(inErrorHandler);
      return left(new FailureSourcePolicyResult(me, emptyMap()));
    });
  }

  private void configureFailingFlow(final ModuleFlowProcessingPhaseTemplate template, RuntimeException failure) throws Exception {
    configureThrowingFlow(template, failure, false);
  }

  private void configureErrorHandlingFailingFlow(final ModuleFlowProcessingPhaseTemplate template, RuntimeException failure)
      throws Exception {
    configureThrowingFlow(template, failure, true);
  }

  private void configureFailureResponse() throws Exception {
    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> {
      throw mockException();
    });
    doAnswer(invocation -> left(new FailureSourcePolicyResult(new MessagingException(invocation.getArgumentAt(0, Event.class),
                                                                                     mockException()),
                                                              emptyMap()))).when(sourcePolicy).process(any());
  }

  private void configureFailingResponseSend() {
    doAnswer(invocation -> {
      final Event event = invocation.getArgumentAt(0, Event.class);
      final MessagingException messagingException = new MessagingException(event, mockException());

      try {
        invocation.getArgumentAt(3, ResponseCompletionCallback.class).responseSentWithFailure(messagingException,
                                                                                              messagingException.getEvent());
      } catch (Exception e) {
        return Mono.error(e);
      }
      return empty();
    }).when(template).sendResponseToClient(any(), any(), any(), any());
  }

  private void configureFailingErrorSend() {
    doAnswer(invocation -> {
      final MessagingException messagingException = invocation.getArgumentAt(0, MessagingException.class);
      try {
        invocation.getArgumentAt(2, ResponseCompletionCallback.class).responseSentWithFailure(messagingException,
                                                                                              messagingException.getEvent());
        return Mono.empty();
      } catch (Exception e) {
        return Mono.error(e);
      }
    }).when(template).sendFailureResponseToClient(any(), any(), any());
  }

  private MessagingException buildFailingFlowException(final Event event, final Exception exception) {
    return new MessagingException(Event.builder(event)
        .error(ErrorBuilder.builder(exception).errorType(ERROR_FROM_FLOW).build())
        .build(), exception);
  }

  private RuntimeException mockException() {
    return new NullPointerException("Mock");
  }

}
