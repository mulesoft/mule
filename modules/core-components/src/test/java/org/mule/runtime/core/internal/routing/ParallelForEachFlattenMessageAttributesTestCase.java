/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.runtime.api.util.MuleSystemProperties.PARALLEL_FOREACH_FLATTEN_MESSAGE_PROPERTY;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.of;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

public class ParallelForEachFlattenMessageAttributesTestCase extends AbstractMuleContextTestCase {

  @Rule
  public SystemProperty systemProperty = new SystemProperty(PARALLEL_FOREACH_FLATTEN_MESSAGE_PROPERTY, "true");
  private final ParallelForEach router = new ParallelForEach();

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    when(componentLocator.find(Location.builder().globalName(APPLE_FLOW).build())).thenReturn(of(mock(Flow.class)));
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @After
  public void tearDown() {
    router.dispose();
  }

  @Test
  @Issue("W-10848628")
  @Description("RoutingPairs should not delete the message's attributes.")
  public void routingPairsShouldKeepAttributes() throws Throwable {
    CoreEvent event = createMessageListEvent();

    MessageProcessorChain nested = mock(MessageProcessorChain.class);
    muleContext.getInjector().inject(router);
    router.setMessageProcessors(singletonList(nested));
    router.setAnnotations(getAppleFlowComponentLocationAnnotations());
    router.initialise();

    List<ForkJoinStrategy.RoutingPair> routingPairs = from(router.getRoutingPairs(event)).collectList().block();
    assertThat(routingPairs, hasSize(2));
    assertThat(routingPairs.get(0).getEvent().getMessage().getPayload(),
               equalTo(((List<Message>) event.getMessage().getPayload().getValue()).get(0).getPayload()));
    assertThat(routingPairs.get(0).getEvent().getMessage().getAttributes(),
               equalTo(((List<Message>) event.getMessage().getPayload().getValue()).get(0).getAttributes()));
    assertThat(routingPairs.get(1).getEvent().getMessage().getPayload(),
               equalTo(((List<Message>) event.getMessage().getPayload().getValue()).get(1).getPayload()));
    assertThat(routingPairs.get(1).getEvent().getMessage().getAttributes(),
               equalTo(((List<Message>) event.getMessage().getPayload().getValue()).get(1).getAttributes()));
  }

  private CoreEvent createMessageListEvent() throws MuleException {
    List<Message> arrayList = new ArrayList<>();
    Message message = Message.builder()
        .payload(TypedValue.of("test"))
        .attributes(new TypedValue<>("{attribute: 1}", JSON_STRING))
        .mediaType(TEXT)
        .build();
    Message message2 = Message.builder()
        .payload(TypedValue.of("test2"))
        .attributes(new TypedValue<>("{attribute: 2}", JSON_STRING))
        .mediaType(TEXT)
        .build();
    arrayList.add(message);
    arrayList.add(message2);
    return getEventBuilder().message(Message.of(arrayList)).build();
  }
}
