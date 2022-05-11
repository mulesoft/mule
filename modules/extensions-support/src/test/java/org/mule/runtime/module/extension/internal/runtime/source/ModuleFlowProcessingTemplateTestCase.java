/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.functional.Either.right;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.internal.execution.SourcePolicyTestUtils.onCallback;
import static org.mule.runtime.core.internal.policy.SourcePolicyContext.from;
import static reactor.core.publisher.Mono.just;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.mockito.ArgumentCaptor;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.execution.CompletableCallback;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.internal.construct.AbstractPipeline;
import org.mule.runtime.core.internal.exception.ExceptionRouter;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.execution.FlowProcessMediator;
import org.mule.runtime.core.internal.execution.MessageProcessContext;
import org.mule.runtime.core.internal.execution.PhaseResultNotifier;
import org.mule.runtime.core.internal.execution.SourceResultAdapter;
import org.mule.runtime.core.internal.execution.SourcePolicyTestUtils;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.core.internal.policy.SourcePolicy;
import org.mule.runtime.core.internal.policy.SourcePolicyContext;
import org.mule.runtime.core.internal.policy.SourcePolicyFailureResult;
import org.mule.runtime.core.internal.policy.SourcePolicySuccessResult;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.reactivestreams.Publisher;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class ModuleFlowProcessingTemplateTestCase extends AbstractMuleContextTestCase {

  @Mock
  private SourceResultAdapter message;

  @Mock
  private CoreEvent event;

  @Mock
  private Processor messageProcessor;

  @Mock
  private SourceCompletionHandler completionHandler;

  @Mock(lenient = true)
  private MessagingException messagingException;

  @Mock
  private Map<String, Object> mockParameters;

  private final RuntimeException runtimeException = new RuntimeException();

  private ExtensionsFlowProcessingTemplate template;

  private final AtomicReference<CoreEvent> atomicEvent = new AtomicReference<>();

  private FlowProcessMediator flowProcessMediator;

  private MessageProcessContext context;

  @Before
  public void before() throws Exception {
    template = new ExtensionsFlowProcessingTemplate(message, messageProcessor, emptyList(), completionHandler);
    doAnswer(onCallback(callback -> callback.complete(null))).when(completionHandler).onCompletion(any(), any(), any());
    doAnswer(onCallback(callback -> callback.complete(null))).when(completionHandler).onFailure(any(), any(), any());
  }

  @Test
  public void getMuleEvent() throws Exception {
    assertThat(template.getSourceMessage(), is(sameInstance(message)));
  }

  @Test
  public void routeEvent() throws Exception {
    template.routeEvent(event);
    verify(messageProcessor).process(event);
  }

  @Test
  public void routeEventAsync() throws Exception {
    when(messageProcessor.apply(any(Publisher.class))).thenReturn(just(event));
    template.routeEventAsync(event);
    verify(messageProcessor).apply(any(Publisher.class));
  }

  @Test
  public void sendResponseToClient() throws Throwable {
    Reference<CompletableCallback<Void>> callbackReference = new Reference<>();
    SourcePolicyTestUtils.<Void>block(callback -> {
      callbackReference.set(callback);
      template.sendResponseToClient(event, mockParameters, callback);
    });

    assertThat(callbackReference, is(notNullValue()));
    verify(completionHandler).onCompletion(same(event), same(mockParameters), same(callbackReference.get()));
  }

  @Test
  public void failedToSendResponseToClient() throws Throwable {
    Reference<Throwable> exceptionReference = new Reference<>();
    Reference<CompletableCallback<Void>> callbackReference = new Reference<>();
    doAnswer(SourcePolicyTestUtils.<Void>onCallback(callback -> {
      callbackReference.set(callback);
      callback.error(runtimeException);
    })).when(completionHandler).onCompletion(same(event), same(mockParameters), any());

    try {
      SourcePolicyTestUtils.<Void>block(callback -> {
        callback = callback.before(new CompletableCallback<Void>() {

          @Override
          public void complete(Void value) {

          }

          @Override
          public void error(Throwable e) {
            exceptionReference.set(e);
          }
        });

        template.sendResponseToClient(event, mockParameters, callback);
      });
      fail("This should have failed");
    } catch (Exception e) {
      assertThat(e, is(sameInstance(runtimeException)));
    }

    verify(completionHandler, never()).onFailure(any(MessagingException.class), same(mockParameters), any());
    assertThat(exceptionReference.get(), equalTo(runtimeException));
  }

  @Test
  public void sendFailureResponseToClient() throws Throwable {
    Reference<CompletableCallback<Void>> callbackReference = new Reference<>();
    SourcePolicyTestUtils.<Void>block(callback -> {
      callbackReference.set(callback);
      template.sendFailureResponseToClient(messagingException, mockParameters, callback);
    });

    assertThat(callbackReference.get(), is(notNullValue()));
    verify(completionHandler).onFailure(messagingException, mockParameters, callbackReference.get());
  }

  @Test
  public void failedToSendFailureResponseToClient() throws Throwable {
    Reference<Throwable> exceptionReference = new Reference<>();
    when(messagingException.getEvent()).thenReturn(event);
    doAnswer(onCallback(callback -> callback.error(runtimeException))).when(completionHandler)
        .onFailure(same(messagingException), same(mockParameters), any());

    try {
      SourcePolicyTestUtils.<Void>block(callback -> {
        callback = callback.before(new CompletableCallback<Void>() {

          @Override
          public void complete(Void value) {

          }

          @Override
          public void error(Throwable e) {
            exceptionReference.set(e);
          }
        });

        template.sendFailureResponseToClient(messagingException, mockParameters, callback);
      });
      fail("This should have failed");
    } catch (Exception e) {
      assertThat(e, is(sameInstance(runtimeException)));
    }

    assertThat(exceptionReference.get(), equalTo(runtimeException));
  }

  @Test
  @Issue("MULE-19869")
  @Description("Set template field to null after phase execution to avoid a leak when creating reactor chains")
  public void templateSetToNullAfterPhaseExecution() throws Exception {
    initFlowProcessMediator();
    flowProcessMediator.process(template, context);
    assertThat(template.getSourceMessage(), is(nullValue()));
  }

  private void initFlowProcessMediator() throws Exception {
    PolicyManager policyManager = mock(PolicyManager.class);
    SourcePolicy sourcePolicy = mock(SourcePolicy.class);
    when(policyManager.createSourcePolicyInstance(any(), any(), any(), any())).thenReturn(sourcePolicy);
    when(policyManager.addSourcePointcutParametersIntoEvent(any(), any(), any())).thenAnswer(inv -> {
      final PolicyPointcutParameters pointcutParams = mock(PolicyPointcutParameters.class);
      final SourcePolicyContext sourcePolicyCtx = new SourcePolicyContext(pointcutParams);

      final InternalEvent invEvent = inv.getArgument(2, InternalEvent.class);
      invEvent.setSourcePolicyContext(sourcePolicyCtx);
      atomicEvent.set(inv.getArgument(2, InternalEvent.class));
      return pointcutParams;
    });
    SourcePolicySuccessResult successResult = mock(SourcePolicySuccessResult.class);
    when(successResult.getResult()).then(invocation -> atomicEvent.get());
    when(successResult.getResponseParameters()).thenReturn(Collections::emptyMap);
    when(successResult.createErrorResponseParameters()).thenReturn(event -> emptyMap());
    SourcePolicyFailureResult failureResult = mock(SourcePolicyFailureResult.class);
    when(failureResult.getMessagingException()).then(invocation -> messagingException);
    when(failureResult.getResult()).then(invocation -> messagingException.getEvent());
    when(failureResult.getErrorResponseParameters()).thenReturn(Collections::emptyMap);
    doAnswer(inv -> {
      CoreEvent event = inv.getArgument(0);
      CompletableCallback<Either<SourcePolicyFailureResult, SourcePolicySuccessResult>> callback = inv.getArgument(2);

      from(event).configure(inv.getArgument(1), callback);

      callback.complete(right(successResult));

      return null;
    }).when(sourcePolicy).process(any(), any(), any());

    PhaseResultNotifier notifier = mock(PhaseResultNotifier.class);
    flowProcessMediator = new FlowProcessMediator(policyManager, notifier);
    initialiseIfNeeded(flowProcessMediator, muleContext);
    startIfNeeded(flowProcessMediator);

    AbstractPipeline flow = mock(AbstractPipeline.class, withSettings().extraInterfaces(Component.class));
    when(flow.getLocation()).thenReturn(DefaultComponentLocation.from("flow"));
    FlowExceptionHandler exceptionHandler = mock(FlowExceptionHandler.class);

    // Call routeError failure callback for success response sending error test cases
    final ArgumentCaptor<Consumer> propagateConsumerCaptor = forClass(Consumer.class);
    ExceptionRouter flowErrorHandlerRouter = mock(ExceptionRouter.class);
    doAnswer(inv -> {
      propagateConsumerCaptor.getValue().accept(inv.getArgument(0));
      return null;
    })
        .when(flowErrorHandlerRouter).accept(any(Exception.class));
    when(exceptionHandler.router(any(Function.class), any(Consumer.class), propagateConsumerCaptor.capture()))
        .thenReturn(flowErrorHandlerRouter);

    final MessageSource source = mock(MessageSource.class);
    when(source.getRootContainerLocation()).thenReturn(builder().globalName("root").build());
    when(source.getLocation()).thenReturn(mock(ComponentLocation.class));

    when(flow.errorRouterForSourceResponseError(any())).thenAnswer(inv -> exceptionHandler
        .router(Function.identity(),
                event -> ((Consumer<Exception>) inv.getArgument(0, Function.class).apply(flow))
                    .accept((Exception) event.getError().get().getCause()),
                error -> ((Consumer<Exception>) inv.getArgument(0, Function.class).apply(flow)).accept((Exception) error)));
    when(flow.getExceptionListener()).thenReturn(exceptionHandler);
    when(flow.getSource()).thenReturn(source);
    when(flow.getMuleContext()).thenReturn(muleContext);

    context = mock(MessageProcessContext.class);
    when(context.getMessageSource()).thenReturn(source);
    when(context.getMessagingExceptionResolver()).thenReturn(new MessagingExceptionResolver(source));
    when(context.getTransactionConfig()).thenReturn(empty());
    when(context.getFlowConstruct()).thenReturn(flow);

    SourceResultAdapter resultAdapter = mock(SourceResultAdapter.class);
    when(resultAdapter.getResult()).thenReturn(Result.builder().build());
    when(resultAdapter.getMediaType()).thenReturn(ANY);

    template = new ExtensionsFlowProcessingTemplate(resultAdapter, messageProcessor, emptyList(), completionHandler);
  }
}
