/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static java.util.Optional.empty;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.from;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.functional.Either;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.tck.junit4.AbstractMuleTestCase;

import com.google.common.collect.ImmutableMap;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;

public class NoSourcePolicyTestCase extends AbstractMuleTestCase {

  private NoSourcePolicy noSourcePolicy;

  private CoreEvent initialEvent = mock(InternalEvent.class, RETURNS_DEEP_STUBS);
  private MessageSourceResponseParametersProcessor respParametersProcessor =
      mock(MessageSourceResponseParametersProcessor.class, RETURNS_DEEP_STUBS);

  private CoreEvent updatedEvent;

  private ReactiveProcessor flowProcessor = Mockito.mock(ReactiveProcessor.class);

  @Before
  public void setUp() throws Exception {
    flowProcessor = coreEventPublisher -> Flux.from(coreEventPublisher).map(event -> {
      updatedEvent = event;
      return updatedEvent;
    });

    noSourcePolicy = new NoSourcePolicy(flowProcessor);

    when(initialEvent.getVariables()).thenReturn(new CaseInsensitiveHashMap<>());
  }

  @Test
  public void process() {
    Map<String, Object> successParameters = responseParameters();

    when(respParametersProcessor.getSuccessfulExecutionResponseParametersFunction().apply(any()))
        .thenReturn(successParameters);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result =
        from(noSourcePolicy.process(initialEvent, respParametersProcessor)).block();

    assertThat(result.getLeft(), nullValue());
    assertThat(result.getRight().getResult(), is(updatedEvent));
    assertThat(result.getRight().getMessageSourceResponseParametersProcessor(), is(respParametersProcessor));
    assertThat(result.getRight().getResponseParameters().get(), is(successParameters));
  }

  @Test
  public void processWithFlowError() {
    flowProcessor = coreEventPublisher -> Flux.from(coreEventPublisher)
        .flatMap(event -> {
          updatedEvent = event;
          return error(new MessagingException(createStaticMessage("message"), event));
        });

    noSourcePolicy = new NoSourcePolicy(flowProcessor);

    Map<String, Object> errorParameters = responseParameters();

    when(respParametersProcessor.getFailedExecutionResponseParametersFunction().apply(any()))
        .thenReturn(errorParameters);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result =
        from(noSourcePolicy.process(initialEvent, respParametersProcessor)).block();

    assertThat(result.getRight(), nullValue());
    assertThat(result.getLeft().getErrorResponseParameters().get(), is(errorParameters));
    assertThat(result.getLeft().getMessagingException().getEvent(), is(updatedEvent));
  }

  @Test
  public void processAfterPolicyDispose() {
    Map<String, Object> errorParameters = responseParameters();
    when(respParametersProcessor.getFailedExecutionResponseParametersFunction().apply(initialEvent))
        .thenReturn(errorParameters);
    noSourcePolicy.dispose();

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result =
        from(noSourcePolicy.process(initialEvent, respParametersProcessor)).block();

    verify(initialEvent.getContext()).error(result.getLeft().getMessagingException());

    assertThat(result.getRight(), nullValue());
    assertThat(result.getLeft().getMessagingException().getEvent().getMessage(), is(initialEvent.getMessage()));
    assertThat(result.getLeft().getMessagingException().getEvent().getContext(), is(initialEvent.getContext()));
    assertThat(result.getLeft().getMessagingException().getEvent().getSecurityContext(),
               is(initialEvent.getSecurityContext()));
    assertThat(result.getLeft().getMessagingException().getEvent().getError(), not(is(empty())));
    assertThat(result.getLeft().getErrorResponseParameters().get(), is(errorParameters));
  }

  private Map<String, Object> responseParameters() {
    return ImmutableMap.of("param", "value");
  }
}
