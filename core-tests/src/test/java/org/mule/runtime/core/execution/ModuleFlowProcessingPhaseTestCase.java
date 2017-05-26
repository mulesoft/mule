/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.execution;

import static java.lang.Boolean.FALSE;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.functional.Either.left;
import static org.mule.runtime.core.api.functional.Either.right;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.exception.ErrorTypeRepositoryFactory.createDefaultErrorTypeRepository;
import static org.mule.runtime.core.execution.ModuleFlowProcessingPhase.ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.registry.MuleRegistry;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.ErrorBuilder;
import org.mule.runtime.core.message.ErrorTypeBuilder;
import org.mule.runtime.core.policy.FailureSourcePolicyResult;
import org.mule.runtime.core.policy.MessageSourceResponseParametersProcessor;
import org.mule.runtime.core.policy.PolicyManager;
import org.mule.runtime.core.policy.SourcePolicy;
import org.mule.runtime.core.policy.SuccessSourcePolicyResult;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.size.SmallTest;

import java.util.Collection;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SmallTest
public class ModuleFlowProcessingPhaseTestCase extends AbstractMuleTestCase {

  private static final ErrorType ERROR_FROM_FLOW =
      ErrorTypeBuilder.builder().parentErrorType(mock(ErrorType.class)).namespace("TEST").identifier("FLOW_FAILED").build();

  @Rule
  public SystemProperty enableSourcePolicies;

  private MuleContext muleContext;
  private FlowConstruct flow;
  private MessageProcessContext context;

  private SourcePolicy sourcePolicy;

  private ModuleFlowProcessingPhase moduleFlowProcessingPhase;

  // TODO MULE-11167 Remove this parameterization once policies sources are non-blocking
  public ModuleFlowProcessingPhaseTestCase(boolean enableSourcePolicies) {
    if (enableSourcePolicies) {
      this.enableSourcePolicies = new SystemProperty(ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY, "true");
    } else {
      this.enableSourcePolicies = new SystemProperty(ENABLE_SOURCE_POLICIES_SYSTEM_PROPERTY, null);
    }
  }

  @Parameters
  public static Collection<Object> data() {
    // TODO MULE-11167 Fix policy cases
    return asList(new Object[] {FALSE/* , TRUE */});
  }

  @Before
  public void before() throws InitialisationException {
    muleContext = mock(MuleContext.class, RETURNS_DEEP_STUBS);
    when(muleContext.getErrorTypeRepository()).thenReturn(createDefaultErrorTypeRepository());
    when(muleContext.getRegistry()).thenReturn(mock(MuleRegistry.class));

    final PolicyManager policyManager = mock(PolicyManager.class);
    sourcePolicy = mock(SourcePolicy.class);
    when(policyManager.createSourcePolicyInstance(any(), any(), any(), any())).thenReturn(sourcePolicy);

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
  }

  @Test
  public void success() throws Exception {
    final ModuleFlowProcessingPhaseTemplate template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));

    configureSuccessfulFlow(template);

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    final PhaseResultNotifier notifier = mock(PhaseResultNotifier.class);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template).sendResponseToClient(any(), any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(EitherMatcher.right(nullValue(MessagingException.class))), any());
  }

  @Test
  public void sourceResponseGenerateErrorType() throws Exception {
    final ModuleFlowProcessingPhaseTemplate template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));

    configureSuccessfulFlow(template);

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> {
      throw new NullPointerException("Mock");
    });

    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    final PhaseResultNotifier notifier = mock(PhaseResultNotifier.class);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener())
        .handleException(any(), argThat(ErrorTypeInEventMatcher.errorType("MULE", "SOURCE_RESPONSE_GENERATE")));
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(allOf(EitherMatcher.left(nullValue(Event.class)),
                                                                      failedWith(ErrorTypeInEventMatcher
                                                                          .errorType("MULE", "SOURCE_RESPONSE_GENERATE")))),
                                                        any());
  }

  @Test
  public void sourceResponseSendErrorType() throws Exception {
    final ModuleFlowProcessingPhaseTemplate template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));

    configureSuccessfulFlow(template);

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());
    doThrow(new NullPointerException("Mock")).when(template).sendResponseToClient(any(), any(), any(), any());
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());


    final PhaseResultNotifier notifier = mock(PhaseResultNotifier.class);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener())
        .handleException(any(), argThat(ErrorTypeInEventMatcher.errorType("MULE", "SOURCE_RESPONSE_SEND")));
    verify(template).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(allOf(EitherMatcher.left(nullValue(Event.class)),
                                                                      failedWith(ErrorTypeInEventMatcher
                                                                          .errorType("MULE", "SOURCE_RESPONSE_SEND")))),
                                                        any());
  }

  @Test
  public void flowError() throws Exception {
    final ModuleFlowProcessingPhaseTemplate template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));
    configureFailingFlow(template);
    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    final PhaseResultNotifier notifier = mock(PhaseResultNotifier.class);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(allOf(EitherMatcher.left(nullValue(Event.class)),
                                                                      failedWith(ErrorTypeInEventMatcher
                                                                          .errorType(ERROR_FROM_FLOW.getNamespace(),
                                                                                     ERROR_FROM_FLOW.getIdentifier())))),
                                                        any());
  }

  @Test
  public void sourceErrorResponseGenerateErrorType() throws Exception {
    final ModuleFlowProcessingPhaseTemplate template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));

    configureSuccessfulFlow(template);

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> {
      throw new NullPointerException("Mock");
    });
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> {
      throw new NullPointerException("Mock");
    });

    final PhaseResultNotifier notifier = mock(PhaseResultNotifier.class);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(allOf(EitherMatcher.left(nullValue(Event.class)),
                                                                      failedWith(ErrorTypeInEventMatcher
                                                                          .errorType("MULE", "SOURCE_ERROR_RESPONSE_GENERATE")))),
                                                        any());
  }

  @Test
  public void sourceErrorResponseSendErrorType() throws Exception {
    final ModuleFlowProcessingPhaseTemplate template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));

    configureSuccessfulFlow(template);

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> {
      throw new NullPointerException("Mock");
    });

    doAnswer(invocation -> {
      final MessagingException messagingException = invocation.getArgumentAt(0, MessagingException.class);
      invocation.getArgumentAt(2, ResponseCompletionCallback.class).responseSentWithFailure(messagingException,
                                                                                            messagingException.getEvent());
      return null;
    }).when(template).sendFailureResponseToClient(any(), any(), any());
    // doThrow(new NullPointerException("Mock")).when(template).sendFailureResponseToClient(any(), any(), any());
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    final PhaseResultNotifier notifier = mock(PhaseResultNotifier.class);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(allOf(EitherMatcher.left(nullValue(Event.class)),
                                                                      failedWith(ErrorTypeInEventMatcher
                                                                          .errorType("MULE", "SOURCE_ERROR_RESPONSE_SEND")))),
                                                        any());
  }

  @Test
  public void sourceErrorResponseGenerateErrorTypeAfterFlowError() throws Exception {
    final ModuleFlowProcessingPhaseTemplate template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));

    configureFailingFlow(template);

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> {
      fail("Must not call success handling methods");
      return emptyMap();
    });
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> {
      throw new NullPointerException("Mock");
    });

    final PhaseResultNotifier notifier = mock(PhaseResultNotifier.class);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template, never()).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(allOf(EitherMatcher.left(nullValue(Event.class)),
                                                                      failedWith(ErrorTypeInEventMatcher
                                                                          .errorType("MULE", "SOURCE_ERROR_RESPONSE_GENERATE")))),
                                                        any());
  }

  @Test
  public void sourceErrorResponseSendErrorTypeAfterFlowError() throws Exception {
    final ModuleFlowProcessingPhaseTemplate template = mock(ModuleFlowProcessingPhaseTemplate.class);
    when(template.getMessage()).thenReturn(Message.of(null));

    configureFailingFlow(template);

    when(template.getSuccessfulExecutionResponseParametersFunction()).thenReturn(event -> {
      fail("Must not call success handling methods");
      return emptyMap();
    });
    doAnswer(invocation -> {
      final MessagingException messagingException = invocation.getArgumentAt(0, MessagingException.class);
      invocation.getArgumentAt(2, ResponseCompletionCallback.class).responseSentWithFailure(messagingException,
                                                                                            messagingException.getEvent());
      return null;
    }).when(template).sendFailureResponseToClient(any(), any(), any());
    // doThrow(new NullPointerException("Mock")).when(template).sendFailureResponseToClient(any(), any(), any());
    when(template.getFailedExecutionResponseParametersFunction()).thenReturn(event -> emptyMap());

    final PhaseResultNotifier notifier = mock(PhaseResultNotifier.class);

    moduleFlowProcessingPhase.runPhase(template, context, notifier);

    verify(flow.getExceptionListener(), never()).handleException(any(), any());
    verify(template, never()).sendResponseToClient(any(), any(), any(), any());
    verify(template).sendFailureResponseToClient(any(), any(), any());
    verify(template).sendAfterTerminateResponseToClient(argThat(allOf(EitherMatcher.left(nullValue(Event.class)),
                                                                      failedWith(ErrorTypeInEventMatcher
                                                                          .errorType("MULE", "SOURCE_ERROR_RESPONSE_SEND")))),
                                                        any());
  }

  private EitherMatcher<Event, MessagingException> failedWith(final ErrorTypeInEventMatcher errorType) {
    return EitherMatcher.right(new TypeSafeMatcher<MessagingException>() {

      @Override
      protected boolean matchesSafely(MessagingException item) {
        return errorType.matches(item.getEvent());
      }

      @Override
      public void describeTo(Description description) {
        errorType.describeTo(description);
      }

    });
  }

  private void configureSuccessfulFlow(final ModuleFlowProcessingPhaseTemplate template) throws Exception {
    when(template.routeEventAsync(any())).then(invocation -> just(invocation.getArgumentAt(0, Event.class)));
    when(sourcePolicy.process(any())).thenAnswer(invocation -> {
      return right(new SuccessSourcePolicyResult(invocation.getArgumentAt(0, Event.class),
                                                 emptyMap(), mock(MessageSourceResponseParametersProcessor.class)));
    });
  }

  private void configureFailingFlow(final ModuleFlowProcessingPhaseTemplate template) throws Exception {
    when(template.routeEventAsync(any())).then(invocation -> {
      final NullPointerException exception = new NullPointerException("Mock");
      final Event event = Event.builder(invocation.getArgumentAt(0, Event.class))
          .error(ErrorBuilder.builder(exception).errorType(ERROR_FROM_FLOW).build())
          .build();
      throw new MessagingException(event, exception);
    });
    when(sourcePolicy.process(any())).thenAnswer(invocation -> {
      final NullPointerException exception = new NullPointerException("Mock");
      final Event event = Event.builder(invocation.getArgumentAt(0, Event.class))
          .error(ErrorBuilder.builder(exception).errorType(ERROR_FROM_FLOW).build())
          .build();
      return left(new FailureSourcePolicyResult(new MessagingException(event,
                                                                       exception,
                                                                       mock(Processor.class)),
                                                emptyMap()));
    });
  }

  private static class ErrorTypeInEventMatcher extends TypeSafeMatcher<Event> {

    private ErrorTypeMatcher matcher;

    public ErrorTypeInEventMatcher(ErrorTypeMatcher matcher) {
      this.matcher = matcher;
    }

    public static ErrorTypeInEventMatcher errorType(ErrorTypeDefinition type) {
      return new ErrorTypeInEventMatcher(ErrorTypeMatcher.errorType(type));
    }

    public static ErrorTypeInEventMatcher errorType(String namespace, String type) {
      return new ErrorTypeInEventMatcher(ErrorTypeMatcher.errorType(namespace, type));
    }

    public static ErrorTypeInEventMatcher errorType(Matcher<String> namespace, Matcher<String> type) {
      return new ErrorTypeInEventMatcher(ErrorTypeMatcher.errorType(namespace, type));
    }

    @Override
    protected boolean matchesSafely(Event item) {
      return matcher.matches(item.getError().get().getErrorType());
    }

    @Override
    public void describeTo(Description description) {
      description.appendText("an Event with ");
      matcher.describeTo(description);
    }
  }

  private static class EitherMatcher<L, R> extends TypeSafeMatcher<Either<L, R>> {

    private Matcher<L> leftMatcher;
    private Matcher<R> rightMatcher;

    public EitherMatcher(Matcher<L> leftMatcher, Matcher<R> rightMatcher) {
      this.leftMatcher = leftMatcher;
      this.rightMatcher = rightMatcher;
    }

    public static <L, R> EitherMatcher<L, R> left(Matcher<L> matcher) {
      return new EitherMatcher(matcher, null);
    }

    public static <L, R> EitherMatcher<L, R> right(Matcher<R> matcher) {
      return new EitherMatcher(null, matcher);
    }

    @Override
    protected boolean matchesSafely(Either<L, R> item) {
      if (leftMatcher != null) {
        return leftMatcher.matches(item.getLeft());
      } else {
        return rightMatcher.matches(item.getRight());
      }
    }

    @Override
    public void describeTo(Description description) {
      if (leftMatcher != null) {
        description.appendText("left ");
        leftMatcher.describeTo(description);
      } else {
        description.appendText("right ");
        rightMatcher.describeTo(description);
      }
    }
  }
}
