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
import static java.util.Optional.of;
import static org.hamcrest.Matchers.instanceOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.core.api.exception.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_ERROR_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_GENERATE;
import static org.mule.runtime.core.api.exception.Errors.ComponentIdentifiers.Handleable.SOURCE_RESPONSE_SEND;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.ANY_IDENTIFIER;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.tck.junit4.matcher.EitherMatcher.leftMatches;
import static org.mule.tck.junit4.matcher.EitherMatcher.rightMatches;
import static org.mule.tck.junit4.matcher.EventMatcher.hasErrorType;
import static org.mule.tck.junit4.matcher.MessagingExceptionMatcher.withEventThat;
import static org.mule.tck.util.MuleContextUtils.mockMuleContext;
import static reactor.core.publisher.Mono.create;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.ErrorBuilder;
import org.mule.runtime.core.internal.message.ErrorTypeBuilder;
import org.mule.runtime.core.internal.policy.MessageSourceResponseParametersProcessor;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.policy.SourcePolicy;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;
import org.mule.runtime.core.privileged.PrivilegedMuleContext;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.EventMatcher;
import org.mule.tck.size.SmallTest;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@SmallTest
public class ModuleFlowProcessingPhaseTestCase extends AbstractMuleTestCase {

  private static final ErrorType ERROR_FROM_FLOW =
      ErrorTypeBuilder.builder().parentErrorType(mock(ErrorType.class)).namespace("TEST").identifier("FLOW_FAILED").build();

  private FlowConstruct flow;
  private MessageProcessContext context;
  private ModuleFlowProcessingPhaseTemplate template;
  private PhaseResultNotifier notifier;

  private SourcePolicy sourcePolicy;
  private SourcePolicySuccessResult successResult;
  private SourcePolicyFailureResult failureResult;
  private CoreEvent event;
  private MessagingException messagingException;
  private RuntimeException mockException;

  private ModuleFlowProcessingPhase moduleFlowProcessingPhase;
  private Supplier<Map<String, Object>> failingParameterSupplier = () -> {
    throw mockException;
  };
  private Function<CoreEvent, Map<String, Object>> failingParameterFunction = event -> {
    throw mockException;
  };
  private PolicyManager policyManager;

  @Parameters
  public static Collection<Object> data() {
    return asList(new Object[] {false, true});
  }

  @Before
  public void before() throws Exception {

    final PrivilegedMuleContext muleContext = (PrivilegedMuleContext) mockMuleContext();
    when(muleContext.getErrorTypeRepository()).thenReturn(createDefaultErrorTypeRepository());
    ErrorTypeLocator errorTypeLocator = mock(ErrorTypeLocator.class);
    when(errorTypeLocator.lookupErrorType(any(Throwable.class)))
        .thenReturn(ErrorTypeBuilder.builder().namespace(CORE_NAMESPACE_NAME).identifier(ANY_IDENTIFIER).build());
    when(muleContext.getErrorTypeLocator()).thenReturn(errorTypeLocator);

    event = mock(CoreEvent.class);
    mockException = mock(RuntimeException.class);

    policyManager = mock(PolicyManager.class);
    sourcePolicy = mock(SourcePolicy.class);
    when(policyManager.createSourcePolicyInstance(any(), any(), any(), any())).thenReturn(sourcePolicy);
    successResult = mock(SourcePolicySuccessResult.class);
    when(successResult.getResult()).then(invocation -> event);
    when(successResult.getResponseParameters()).thenReturn(() -> emptyMap());
    when(successResult.createErrorResponseParameters()).thenReturn(event -> emptyMap());
    failureResult = mock(SourcePolicyFailureResult.class);
    when(failureResult.getMessagingException()).then(invocation -> messagingException);
    when(failureResult.getErrorResponseParameters()).thenReturn(() -> emptyMap());
    when(sourcePolicy.process(any())).thenAnswer(invocation -> {
      event = invocation.getArgumentAt(0, CoreEvent.class);
      return just(right(successResult));
    });

    moduleFlowProcessingPhase = new ModuleFlowProcessingPhase(policyManager);
    moduleFlowProcessingPhase.setMuleContext(muleContext);
    initialiseIfNeeded(moduleFlowProcessingPhase, muleContext);

    flow = mock(FlowConstruct.class, withSettings().extraInterfaces(Component.class));
    final FlowExceptionHandler exceptionHandler = mock(FlowExceptionHandler.class);
    when(flow.getExceptionListener()).thenReturn(exceptionHandler);
    when(exceptionHandler.apply(any()))
        .thenAnswer(invocationOnMock -> error(invocationOnMock.getArgumentAt(0, MessagingException.class)));
    when(flow.getMuleContext()).thenReturn(muleContext);

    context = mock(MessageProcessContext.class);
    final MessageSource source = mock(MessageSource.class);
    when(source.getRootContainerName()).thenReturn("root");
    when(source.getLocation()).thenReturn(mock(ComponentLocation.class));
    when(context.getMessageSource()).thenReturn(source);
    when(context.getTransactionConfig()).thenReturn(empty());
    when(muleContext.getConfigurationComponentLocator().find(any(Location.class))).thenReturn(of(flow));

    template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));
    when(template.sendResponseToClient(any(), any())).thenAnswer(invocation -> Mono.empty());
    when(template.sendFailureResponseToClient(any(), any())).thenAnswer(invocation -> Mono.empty());

    notifier = mock(PhaseResultNotifier.class);
  }

  @Test
  public void success() throws Exception {
    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifySuccess();
  }

  @Test
  public void successDelayedResponseCompletion() throws Exception {
    final Reference<MonoSink> sinkReference = new Reference<>();
    when(template.sendResponseToClient(any(), any())).thenAnswer(invocation -> create(sinkReference::set));

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    sinkReference.get().success();
    verifySuccess();
  }

  @Test
  public void successResponseParametersError() throws Exception {
    when(successResult.getResponseParameters()).thenReturn(failingParameterSupplier);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseGenerate());
    verify(template, never()).sendResponseToClient(any(), any());
    verify(template).sendFailureResponseToClient(any(), any());
    verify(template).afterPhaseExecution(argThat(leftMatches(Matchers.any(MessagingException.class))));
    // The error handler is called with the appropriate type, but for the termination it counts as successful if the error
    // response
    // was sent.
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  @Test
  public void successResponseSendError() throws Exception {
    when(template.sendResponseToClient(any(), any())).thenReturn(error(mockException));

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseSend());
    verify(template).sendResponseToClient(any(), any());
    verify(template).sendFailureResponseToClient(any(), any());
    // The error handler is called with the appropriate type, but for the termination it counts as successful if the error
    // response
    // was sent.
    verify(template).afterPhaseExecution(argThat(leftMatches(Matchers.any(MessagingException.class))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  @Test
  public void successResponseParameterAndErrorResponseParameterError() throws Exception {
    when(successResult.getResponseParameters()).thenReturn(failingParameterSupplier);
    when(successResult.createErrorResponseParameters()).thenReturn(failingParameterFunction);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseGenerate());
    verify(template, never()).sendResponseToClient(any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any());
    verify(template)
        .afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseGenerate()))));
    verify(template).afterPhaseExecution(any());
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void successResponseParameterErrorAndErrorResponseSendError() throws Exception {
    when(successResult.getResponseParameters()).thenReturn(failingParameterSupplier);

    when(template.sendFailureResponseToClient(any(), any())).thenReturn(error(mockException));

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowErrorHandler(isErrorTypeSourceResponseGenerate());
    verify(template, never()).sendResponseToClient(any(), any());
    verify(template).sendFailureResponseToClient(any(), any());
    verify(template).afterPhaseExecution(any());
    verify(template)
        .afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseSend()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void failure() throws Exception {
    configureFailingFlow(mockException);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verifyFlowError();
  }

  @Test
  public void failureInErrorHandler() throws Exception {
    configureErrorHandlingFailingFlow(mockException);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any());
    verify(template)
        .afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseGenerate()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void failureDelayedErrorResponseSendCompletion() throws Exception {
    Reference<MonoSink> sinkReference = new Reference<>();

    reset(template);
    when(template.getMessage()).thenReturn(Message.of(null));
    when(template.sendFailureResponseToClient(any(), any())).thenAnswer(invocation -> create(sinkReference::set));

    configureFailingFlow(mockException);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    sinkReference.get().success();
    verifyFlowError();
  }

  @Test
  public void failureDelayedErrorResponseSendFailure() throws Exception {
    Reference<MonoSink> sinkReference = new Reference<>();

    configureFailingFlow(mockException);
    when(template.sendFailureResponseToClient(any(), any())).thenAnswer(invocation -> create(sinkReference::set));

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(template, never()).afterPhaseExecution(any());

    sinkReference.get().error(mockException);

    verify(template, never()).sendResponseToClient(any(), any());
    verify(template).sendFailureResponseToClient(any(), any());
    verify(template)
        .afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseSend()))));
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void failureErrorResponseParameterError() throws Exception {
    configureFailingFlow(mockException);
    when(failureResult.getErrorResponseParameters()).thenReturn(failingParameterSupplier);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any());
    verify(template)
        .afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseGenerate()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }


  @Test
  public void failureErrorResponseSendError() throws Exception {
    configureFailingFlow(mockException);
    when(template.sendFailureResponseToClient(any(), any())).thenReturn(error(mockException));

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any());
    verify(template).sendFailureResponseToClient(any(), any());
    verify(template)
        .afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeSourceErrorResponseSend()))));
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  @Test
  public void failurePolicyManager() throws Exception {
    when(policyManager.createSourcePolicyInstance(any(Component.class), any(CoreEvent.class), any(Processor.class),
                                                  any(MessageSourceResponseParametersProcessor.class))).thenThrow(mockException);
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(coreEvent -> emptyMap());

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any());
    verify(template).sendFailureResponseToClient(any(), any());
    verify(notifier, never()).phaseSuccessfully();
    verify(notifier).phaseFailure(argThat(instanceOf(mockException.getClass())));
  }

  private void verifySuccess() {
    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template).sendResponseToClient(any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any());
    verify(template).afterPhaseExecution(argThat(rightMatches(Matchers.any(CoreEvent.class))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
    verify(template).afterPhaseExecution(any());
  }

  private void verifyFlowError() {
    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any());
    verify(template).sendFailureResponseToClient(any(), any());
    verify(template).afterPhaseExecution(argThat(leftMatches(withEventThat(isErrorTypeFlowFailure()))));
    verify(notifier).phaseSuccessfully();
    verify(notifier, never()).phaseFailure(any());
  }

  private void verifyFlowErrorHandler(final EventMatcher errorHandlerEventMatcher) {
    verify(flow.getExceptionListener()).apply(argThat(withEventThat(errorHandlerEventMatcher)));
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

  private void configureThrowingFlow(RuntimeException failure, boolean inErrorHandler) {
    when(sourcePolicy.process(any())).thenAnswer(invocation -> {
      messagingException = buildFailingFlowException(invocation.getArgumentAt(0, CoreEvent.class), failure);
      messagingException.setInErrorHandler(inErrorHandler);
      return just(left(failureResult));
    });
  }

  private void configureFailingFlow(RuntimeException failure) {
    configureThrowingFlow(failure, false);
  }

  private void configureErrorHandlingFailingFlow(RuntimeException failure) {
    configureThrowingFlow(failure, true);
  }


  private MessagingException buildFailingFlowException(final CoreEvent event, final Exception exception) {
    return new MessagingException(CoreEvent.builder(event)
        .error(ErrorBuilder.builder(exception).errorType(ERROR_FROM_FLOW).build())
        .build(), exception);
  }

}
