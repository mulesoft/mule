/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.internal.execution.SourcePolicyTestUtils.block;
import static reactor.core.publisher.Mono.error;

import org.mule.runtime.api.functional.Either;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.util.CaseInsensitiveHashMap;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.policy.api.PolicyPointcutParameters;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.ImmutableMap;

import reactor.core.publisher.Flux;

public class NoSourcePolicyTestCase extends AbstractMuleTestCase {

  private NoSourcePolicy noSourcePolicy;

  private final InternalEvent initialEvent = mock(InternalEvent.class, RETURNS_DEEP_STUBS);
  private final MessageSourceResponseParametersProcessor respParametersProcessor =
      mock(MessageSourceResponseParametersProcessor.class, RETURNS_DEEP_STUBS);

  private CoreEvent updatedEvent;
  private ReactiveProcessor flowProcessor = Mockito.mock(ReactiveProcessor.class);
  private SourcePolicyContext sourcePolicyContext;

  @Before
  public void setUp() throws Exception {
    sourcePolicyContext = new SourcePolicyContext(mock(PolicyPointcutParameters.class));
    flowProcessor = coreEventPublisher -> Flux.from(coreEventPublisher).map(event -> {
      updatedEvent = event;
      return updatedEvent;
    });

    noSourcePolicy = new NoSourcePolicy(flowProcessor);

    when(initialEvent.getVariables()).thenReturn(new CaseInsensitiveHashMap<>());
    when(initialEvent.getSourcePolicyContext()).thenReturn((EventInternalContext) sourcePolicyContext);
  }

  @Test
  public void process() throws Throwable {
    Map<String, Object> successParameters = responseParameters();

    when(respParametersProcessor.getSuccessfulExecutionResponseParametersFunction().apply(any()))
        .thenReturn(successParameters);

    Either<SourcePolicyFailureResult, SourcePolicySuccessResult> result =
        block(callback -> noSourcePolicy.process(initialEvent, respParametersProcessor, callback));

    assertThat(result.getLeft(), nullValue());
    assertThat(result.getRight().getResult(), is(updatedEvent));
    assertThat(result.getRight().getMessageSourceResponseParametersProcessor(), is(respParametersProcessor));
    assertThat(result.getRight().getResponseParameters().get(), is(successParameters));
  }

  @Test
  public void processWithFlowError() throws Throwable {
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
        block(callback -> noSourcePolicy.process(initialEvent, respParametersProcessor, callback));

    assertThat(result.getRight(), nullValue());
    assertThat(result.getLeft().getErrorResponseParameters().get(), is(errorParameters));
    assertThat(result.getLeft().getMessagingException().getEvent(), is(updatedEvent));
  }

  private Map<String, Object> responseParameters() {
    return ImmutableMap.of("param", "value");
  }
}
