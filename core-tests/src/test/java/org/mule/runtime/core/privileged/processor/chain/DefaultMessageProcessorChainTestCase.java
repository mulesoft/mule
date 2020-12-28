/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.privileged.processor.chain;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.randomNumeric;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.component.location.ConfigurationComponentLocator.REGISTRY_KEY;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_POST_INVOKE;
import static org.mule.runtime.api.notification.MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE;
import static org.mule.runtime.core.api.construct.Flow.builder;
import static org.mule.runtime.core.api.event.EventContextFactory.create;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_INTENSIVE;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.newChain;
import static org.mule.runtime.core.privileged.processor.MessageProcessors.processToApply;
import static org.mule.runtime.dsl.api.component.config.DefaultComponentLocation.fromSingleComponent;
import static org.mule.tck.MuleTestUtils.APPLE_FLOW;
import static org.mule.tck.MuleTestUtils.createAndRegisterFlow;
import static org.mule.tck.junit4.AbstractReactiveProcessorTestCase.Mode.BLOCKING;
import static org.mule.tck.junit4.AbstractReactiveProcessorTestCase.Mode.NON_BLOCKING;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.core.publisher.Flux.from;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.MessageProcessorNotificationListener;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;
import org.mule.runtime.core.api.exception.NullExceptionHandler;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.util.ObjectUtils;
import org.mule.runtime.core.internal.exception.MessagingException;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.processor.strategy.BlockingProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.DirectProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.ProactorStreamEmitterProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.StreamEmitterProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.TransactionAwareProactorStreamEmitterProcessingStrategyFactory;
import org.mule.runtime.core.internal.processor.strategy.TransactionAwareStreamEmitterProcessingStrategyFactory;
import org.mule.runtime.core.internal.routing.ChoiceRouter;
import org.mule.runtime.core.internal.routing.ScatterGatherRouter;
import org.mule.runtime.core.privileged.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.privileged.processor.InternalProcessor;
import org.mule.runtime.core.privileged.processor.MessageProcessorBuilder;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder.DefaultMessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.DefaultMessageProcessorChainBuilder.InterceptingMessageProcessorChain;
import org.mule.tck.junit4.AbstractReactiveProcessorTestCase;
import org.mule.tck.size.SmallTest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;

import io.qameta.allure.Issue;
import reactor.core.publisher.Flux;

@RunWith(Parameterized.class)
@SmallTest
@SuppressWarnings("deprecation")
public class DefaultMessageProcessorChainTestCase extends AbstractReactiveProcessorTestCase {

  private static final Logger LOGGER = getLogger(DefaultMessageProcessorChainTestCase.class);

  protected MuleContext muleContext;

  private final AtomicInteger nonBlockingProcessorsExecuted = new AtomicInteger(0);
  private final ProcessingStrategyFactory processingStrategyFactory;
  private final RuntimeException illegalStateException = new IllegalStateException();

  @Rule
  public ExpectedException expectedException = none();

  @Parameterized.Parameters(name = "{0}, {2}")
  public static Collection<Object[]> parameters() {
    return asList(new Object[][] {
        {"TransactionAwareStreamEmitterProcessingStrategyFactory",
            new TransactionAwareStreamEmitterProcessingStrategyFactory(), BLOCKING},
        {"TransactionAwareProactorStreamEmitterProcessingStrategyFactory",
            new TransactionAwareProactorStreamEmitterProcessingStrategyFactory(), BLOCKING},
        {"StreamEmitterProcessingStrategyFactory",
            new StreamEmitterProcessingStrategyFactory(), BLOCKING},
        {"ProactorStreamEmitterProcessingStrategyFactory",
            new ProactorStreamEmitterProcessingStrategyFactory(), BLOCKING},
        {"BlockingProcessingStrategyFactory",
            new BlockingProcessingStrategyFactory(), BLOCKING},
        {"DirectProcessingStrategyFactory",
            new DirectProcessingStrategyFactory(), BLOCKING},
        {"TransactionAwareStreamEmitterProcessingStrategyFactory",
            new TransactionAwareStreamEmitterProcessingStrategyFactory(), NON_BLOCKING},
        {"TransactionAwareProactorStreamEmitterProcessingStrategyFactory",
            new TransactionAwareProactorStreamEmitterProcessingStrategyFactory(), NON_BLOCKING},
        {"StreamEmitterProcessingStrategyFactory",
            new StreamEmitterProcessingStrategyFactory(), NON_BLOCKING},
        {"ProactorStreamEmitterProcessingStrategyFactory",
            new ProactorStreamEmitterProcessingStrategyFactory(), NON_BLOCKING},
        {"BlockingProcessingStrategyFactory",
            new BlockingProcessingStrategyFactory(), NON_BLOCKING},
        {"DirectProcessingStrategyFactory",
            new DirectProcessingStrategyFactory(), NON_BLOCKING}});
  }

  private Flow flow;

  public DefaultMessageProcessorChainTestCase(String psName, ProcessingStrategyFactory processingStrategyFactory, Mode mode) {
    super(mode);
    this.processingStrategyFactory = processingStrategyFactory;
  }

  @Before
  public void before() throws MuleException {
    nonBlockingProcessorsExecuted.set(0);
    muleContext = spy(super.muleContext);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.isContainerMode()).thenReturn(false);
    when(muleConfiguration.getId()).thenReturn(randomNumeric(3));
    when(muleConfiguration.getShutdownTimeout()).thenReturn(1000L);
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);

    flow = builder("flow", muleContext).processingStrategyFactory(processingStrategyFactory).build();
    flow.setAnnotations(singletonMap(LOCATION_KEY, fromSingleComponent("flow")));
    flow.initialise();
    flow.start();
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    return singletonMap(REGISTRY_KEY, componentLocator);
  }

  @After
  public void after() throws MuleException {
    flow.stop();
    flow.dispose();
  }

  @Test
  public void testMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"));
    assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  /*
   * Any MP returns null: - Processing doesn't proceed - Result of chain is Null
   */
  @Test
  public void testMPChainWithNullReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

    AppendingMP mp1 = getAppendingMP("1");
    AppendingMP mp2 = getAppendingMP("2");
    ReturnNullMP nullmp = new ReturnNullMP();
    AppendingMP mp3 = getAppendingMP("3");
    builder.chain(mp1, mp2, nullmp, mp3);

    CoreEvent requestEvent = getTestEventUsingFlow("0");
    assertNull(process(builder.build(), requestEvent));

    // mp1
    assertSame(requestEvent.getMessage(), mp1.event.getMessage());
    assertNotSame(mp1.event, mp1.resultEvent);
    assertEquals("01", mp1.resultEvent.getMessage().getPayload().getValue());

    // mp2
    assertSame(mp1.resultEvent.getMessage(), mp2.event.getMessage());
    assertNotSame(mp2.event, mp2.resultEvent);
    assertEquals("012", mp2.resultEvent.getMessage().getPayload().getValue());

    // nullmp
    assertSame(mp2.resultEvent.getMessage(), nullmp.event.getMessage());
    assertEquals("012", nullmp.event.getMessage().getPayload().getValue());

    // mp3
    assertNull(mp3.event);
  }

  /*
   * Any MP returns null: - Processing doesn't proceed - Result of chain is Null
   */
  @Test
  public void testMPChainWithVoidReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

    AppendingMP mp1 = getAppendingMP("1");
    AppendingMP mp2 = getAppendingMP("2");
    ReturnVoidMP voidmp = new ReturnVoidMP();
    AppendingMP mp3 = getAppendingMP("3");
    builder.chain(mp1, mp2, voidmp, mp3);

    CoreEvent requestEvent = getTestEventUsingFlow("0");
    assertEquals("0123", process(builder.build(), requestEvent).getMessage().getPayload().getValue());

    // mp1
    // assertSame(requestEvent, mp1.event);
    assertNotSame(mp1.event, mp1.resultEvent);

    // mp2
    // assertSame(mp1.resultEvent, mp2.event);
    assertNotSame(mp2.event, mp2.resultEvent);

    // void mp
    assertEquals(mp2.resultEvent.getMessage(), voidmp.event.getMessage());

    // mp3
    assertThat(mp3.event.getMessage().getPayload().getValue(), equalTo(mp2.resultEvent.getMessage().getPayload().getValue()));
    assertEquals(mp3.event.getMessage().getPayload().getValue(), "012");
  }

  @Test
  public void testMPChainWithNullReturnAtEnd() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnNullMP());
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  public void testMPChainWithVoidReturnAtEnd() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnVoidMP());
    assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testMPChainWithBuilder() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"));
    builder.chain((MessageProcessorBuilder) () -> getAppendingMP("2"));
    builder.chain(getAppendingMP("3"));
    assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testInterceptingMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"), new AppendingInterceptingMP("3"));
    assertEquals("0before1before2before3after3after2after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testInterceptingMPChainWithNullReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

    AppendingInterceptingMP lastMP = new AppendingInterceptingMP("3");

    builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"), new ReturnNullInterceptongMP(), lastMP);
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    assertFalse(lastMP.invoked);
  }

  @Test
  public void testInterceptingMPChainWithVoidReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

    AppendingInterceptingMP lastMP = new AppendingInterceptingMP("3");

    builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"), new ReturnNullInterceptongMP(), lastMP);
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    assertFalse(lastMP.invoked);
  }

  @Test
  public void testMixedMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                  getAppendingMP("5"));
    assertEquals("0before123before45after4after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  // Whenever there is a IMP that returns null the final result is null
  public void testMixedMPChainWithNullReturn1() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new ReturnNullInterceptongMP(), getAppendingMP("2"), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  // Whenever there is a IMP that returns null the final result is null
  public void testMixedMPChainWithVoidReturn1() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new ReturnVoidMPInterceptongMP(), getAppendingMP("2"), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue(),
               equalTo("0before1after1"));
  }

  @Test
  // Whenever there is a IMP that returns null the final result is null
  public void testMixedMPChainWithNullReturn2() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnNullInterceptongMP(), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  // Whenever there is a IMP that returns null the final result is null
  public void testMixedMPChainWithVoidlReturn2() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnVoidMPInterceptongMP(), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertEquals("0before12after1", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  // A simple MP that returns null does not affect flow as long as it's not at the
  // end
  public void testMixedMPChainWithNullReturn3() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new ReturnNullMP(), getAppendingMP("2"), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  // A simple MP that returns null does not affect flow as long as it's not at the
  // end
  public void testMixedMPChainWithVoidReturn3() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new ReturnVoidMP(), getAppendingMP("2"), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertEquals("0before123before45after4after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  // A simple MP that returns null does not affect flow as long as it's not at the
  // end
  public void testMixedMPChainWithNullReturn4() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnNullMP(), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  // A simple MP that returns null does not affect flow as long as it's not at the
  // end
  public void testMixedMPChainWithVoidReturn4() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnVoidMP(), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertEquals("0before123before45after4after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  // A simple MP that returns null does not affect flow as long as it's not at the
  // end
  public void testMixedMPChainWithNullReturn5() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnNullMP(),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  // A simple MP that returns null does not affect flow as long as it's not at the
  // end
  public void testMixedMPChainWithVoidReturn5() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnVoidMP(),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertEquals("0before123before45after4after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  // A simple MP at the end of a single level chain causes chain to return null
  public void testMixedMPChainWithNullReturnAtEnd() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                  getAppendingMP("5"), new ReturnNullMP());
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  // A simple MP at the end of a single level chain causes chain to return null
  public void testMixedMPChainWithVoidReturnAtEnd() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                  getAppendingMP("5"), new ReturnVoidMP());
    assertEquals("0before123before45after4after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testNestedMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"),
                  new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b")).build(),
                  getAppendingMP("2"));
    assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testNestedMPChainWithNullReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(
                  getAppendingMP("1"), new DefaultMessageProcessorChainBuilder()
                      .chain(getAppendingMP("a"), new ReturnNullMP(), getAppendingMP("b")).build(),
                  new ReturnNullMP(), getAppendingMP("2"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  public void testNestedMPChainWithVoidReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(
                  getAppendingMP("1"), new DefaultMessageProcessorChainBuilder()
                      .chain(getAppendingMP("a"), new ReturnVoidMP(), getAppendingMP("b")).build(),
                  new ReturnVoidMP(), getAppendingMP("2"));
    assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testNestedMPChainWithNullReturnAtEndOfNestedChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), new DefaultMessageProcessorChainBuilder()
        .chain(getAppendingMP("a"), getAppendingMP("b"), new ReturnNullMP()).build(), getAppendingMP("2"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  public void testNestedMPChainWithVoidReturnAtEndOfNestedChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), new DefaultMessageProcessorChainBuilder()
        .chain(getAppendingMP("a"), getAppendingMP("b"), new ReturnVoidMP()).build(), getAppendingMP("2"));
    assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testNestedMPChainWithNullReturnAtEndOfNestedChainWithNonInterceptingWrapper() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final MessageProcessorChain nested =
        new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b"), new ReturnNullMP())
            .build();
    nested.setMuleContext(muleContext);
    builder.chain(getAppendingMP("1"), event -> nested.process(event), getAppendingMP("2"));
    assertNull("012", process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  public void testNestedMPChainWithVoidReturnAtEndOfNestedChainWithNonInterceptingWrapper() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final MessageProcessorChain nested =
        new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b"), new ReturnVoidMP())
            .build();
    nested.setMuleContext(muleContext);
    builder.chain(getAppendingMP("1"), event -> nested.process(InternalEvent.builder(event)
        .message(event.getMessage()).build()), getAppendingMP("2"));
    assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testNestedInterceptingMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"),
                  new DefaultMessageProcessorChainBuilder()
                      .chain(new AppendingInterceptingMP("a"), new AppendingInterceptingMP("b")).build(),
                  new AppendingInterceptingMP("2"));
    assertEquals("0before1beforeabeforebafterbafterabefore2after2after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testNestedInterceptingMPChainWithNullReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"),
                  new DefaultMessageProcessorChainBuilder()
                      .chain(new AppendingInterceptingMP("a"), new ReturnNullInterceptongMP(), new AppendingInterceptingMP("b"))
                      .build(),
                  new AppendingInterceptingMP("2"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
  }

  @Test
  public void testNestedInterceptingMPChainWithVoidReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"),
                  new DefaultMessageProcessorChainBuilder()
                      .chain(new AppendingInterceptingMP("a"), new ReturnVoidMPInterceptongMP(), new AppendingInterceptingMP("b"))
                      .build(),
                  new AppendingInterceptingMP("2"));
    assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue(),
               equalTo("0before1beforeaafterabefore2after2after1"));
  }

  @Test
  public void testNestedMixedMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"),
                  new DefaultMessageProcessorChainBuilder()
                      .chain(new AppendingInterceptingMP("a"), getAppendingMP("b")).build(),
                  new AppendingInterceptingMP("2"));
    assertEquals("01beforeabafterabefore2after2",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testInterceptingMPChainStopFlow() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2", true), new AppendingInterceptingMP("3"));
    assertEquals("0before1after1", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  /**
   * Note: Stopping the flow of a nested chain causes the nested chain to return early, but does not stop the flow of the parent
   * chain.
   */
  @Test
  public void testNestedInterceptingMPChainStopFlow() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"),
                  new DefaultMessageProcessorChainBuilder()
                      .chain(new AppendingInterceptingMP("a", true), new AppendingInterceptingMP("b")).build(),
                  new AppendingInterceptingMP("3"));
    assertEquals("0before1before3after3after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());
  }

  @Test
  public void testMPChainLifecycle() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    AppendingInterceptingMP mp1 = new AppendingInterceptingMP("1");
    AppendingInterceptingMP mp2 = new AppendingInterceptingMP("2");
    Processor chain = builder.chain(mp1, mp2).build();
    initialiseIfNeeded(chain, muleContext);
    ((Lifecycle) chain).start();
    ((Lifecycle) chain).stop();
    ((Lifecycle) chain).dispose();
    assertLifecycle(mp1);
    assertLifecycle(mp2);
  }

  @Test
  public void testNestedMPChainLifecycle() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    DefaultMessageProcessorChainBuilder nestedBuilder = new DefaultMessageProcessorChainBuilder();
    AppendingInterceptingMP mp1 = new AppendingInterceptingMP("1");
    AppendingInterceptingMP mp2 = new AppendingInterceptingMP("2");
    AppendingInterceptingMP mpa = new AppendingInterceptingMP("a");
    AppendingInterceptingMP mpb = new AppendingInterceptingMP("b");
    Processor chain = builder.chain(mp1, nestedBuilder.chain(mpa, mpb).build(), mp2).build();
    initialiseIfNeeded(chain, muleContext);
    ((Lifecycle) chain).start();
    ((Lifecycle) chain).stop();
    ((Lifecycle) chain).dispose();
    assertLifecycle(mp1);
    assertLifecycle(mp2);
    assertLifecycle(mpa);
    assertLifecycle(mpb);
  }

  @Test
  public void testNoneIntercepting() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new TestNonIntercepting(), new TestNonIntercepting(), new TestNonIntercepting());
    CoreEvent result = process(builder.build(), getTestEventUsingFlow(""));
    assertEquals("MessageProcessorMessageProcessorMessageProcessor", result.getMessage().getPayload().getValue());
  }

  @Test
  public void testAllIntercepting() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new TestIntercepting(), new TestIntercepting(), new TestIntercepting());
    CoreEvent result = process(builder.build(), getTestEventUsingFlow(""));
    assertEquals("InterceptingMessageProcessorInterceptingMessageProcessorInterceptingMessageProcessor",
                 result.getMessage().getPayload().getValue());
  }

  @Test
  public void testMix() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(), new TestIntercepting(),
                  new TestNonIntercepting(), new TestNonIntercepting());
    CoreEvent result = process(builder.build(), getTestEventUsingFlow(""));
    assertEquals("InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
                 result.getMessage().getPayload().getValue());
  }

  @Test
  @Issue("MULE-19085")
  public void testMixErrorHandlerSetOnce() throws Exception {
    final FlowExceptionHandler errorHandler = mock(FlowExceptionHandler.class);

    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new TestIntercepting(), new TestNonIntercepting())
        .setMessagingExceptionHandler(errorHandler);
    final InterceptingMessageProcessorChain chain = (InterceptingMessageProcessorChain) builder.build();

    final DefaultMessageProcessorChain intercepting =
        (DefaultMessageProcessorChain) chain.getMessageProcessorsForLifecycle().get(0);
    final DefaultMessageProcessorChain nonIntercepting =
        (DefaultMessageProcessorChain) chain.getMessageProcessorsForLifecycle().get(1);

    assertThat(chain.getMessagingExceptionHandler(), is(NullExceptionHandler.getInstance()));
    assertThat(intercepting.getMessagingExceptionHandler(), is(errorHandler));
    assertThat(nonIntercepting.getMessagingExceptionHandler(), is(NullExceptionHandler.getInstance()));
  }

  @Test
  public void testMixStaticFactory() throws Exception {
    MessageProcessorChain chain =
        newChain(empty(), new TestIntercepting(), new TestNonIntercepting(),
                 new TestNonIntercepting(), new TestIntercepting(), new TestNonIntercepting(),
                 new TestNonIntercepting());
    CoreEvent result = process(chain, getTestEventUsingFlow(""));
    assertEquals("InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
                 result.getMessage().getPayload().getValue());
  }

  @Test
  public void testMix2() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new TestNonIntercepting(), new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(),
                  new TestNonIntercepting(), new TestIntercepting());
    CoreEvent result = process(builder.build(), getTestEventUsingFlow(""));
    assertEquals("MessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
                 result.getMessage().getPayload().getValue());
  }

  @Test
  public void testMix2StaticFactory() throws Exception {
    MessageProcessorChain chain =
        newChain(empty(), new TestNonIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(),
                 new TestIntercepting());
    CoreEvent result = process(chain, getTestEventUsingFlow(""));
    assertEquals("MessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
                 result.getMessage().getPayload().getValue());
  }

  @Test
  public void testAll() throws Exception {
    createAndRegisterFlow(muleContext, APPLE_FLOW, componentLocator);
    ScatterGatherRouter scatterGatherRouter = new ScatterGatherRouter();
    scatterGatherRouter.setAnnotations(getAppleFlowComponentLocationAnnotations());
    scatterGatherRouter
        .setRoutes(asList(newChain(empty(), getAppendingMP("1")), newChain(empty(), getAppendingMP("2")),
                          newChain(empty(), getAppendingMP("3"))));

    CoreEvent event = getTestEventUsingFlow("0");
    final MessageProcessorChain chain = newChain(empty(), singletonList(scatterGatherRouter));
    Message result = process(chain, CoreEvent.builder(event).message(event.getMessage()).build()).getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(Map.class));
    Map<String, Message> resultMessage = (Map<String, Message>) result.getPayload().getValue();
    assertThat(resultMessage.values().stream().map(msg -> msg.getPayload().getValue()).collect(toList()).toArray(),
               is(equalTo(new String[] {"01", "02", "03"})));

    scatterGatherRouter.stop();
    scatterGatherRouter.dispose();
  }

  @Test
  public void testChoice() throws Exception {
    ChoiceRouter choiceRouter = new ChoiceRouter();
    choiceRouter.setAnnotations(getAppleFlowComponentLocationAnnotations());
    choiceRouter.setAnnotations(singletonMap(LOCATION_KEY, TEST_CONNECTOR_LOCATION));
    choiceRouter.addRoute("true", newChain(empty(), getAppendingMP("1")));
    choiceRouter.addRoute("true", newChain(empty(), getAppendingMP("2")));
    choiceRouter.addRoute("true", newChain(empty(), getAppendingMP("3")));
    initialiseIfNeeded(choiceRouter, muleContext);

    try {
      assertThat(process(newChain(empty(), choiceRouter), getTestEventUsingFlow("0")).getMessage().getPayload().getValue(),
                 equalTo("01"));
    } finally {
      disposeIfNeeded(choiceRouter, LOGGER);
    }
  }

  @Test
  public void testExceptionAfter() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), new ExceptionThrowingMessageProcessor(illegalStateException));
    expectedException.expect(is(illegalStateException));
    process(builder.build(), getTestEventUsingFlow("0"));
  }

  @Test
  public void testExceptionBefore() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new ExceptionThrowingMessageProcessor(illegalStateException), getAppendingMP("1"));
    expectedException.expect(is(illegalStateException));
    process(builder.build(), getTestEventUsingFlow("0"));
  }

  @Test
  public void testExceptionBetween() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), new ExceptionThrowingMessageProcessor(illegalStateException), getAppendingMP("2"));
    expectedException.expect(is(illegalStateException));
    process(builder.build(), getTestEventUsingFlow("0"));
  }

  @Test
  public void testSuccessNotifications() throws Exception {
    List<MessageProcessorNotification> notificationList = new ArrayList<>();
    setupMessageProcessorNotificationListener(notificationList);
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"));
    final CoreEvent inEvent = getTestEventUsingFlow("0");
    final String resultPayload = "01";
    assertThat(process(builder.build(), inEvent).getMessage().getPayload().getValue(), equalTo(resultPayload));
    assertThat(notificationList, hasSize(2));
    MessageProcessorNotification preNotification = notificationList.get(0);
    MessageProcessorNotification postNotification = notificationList.get(1);
    assertPreNotification(inEvent, preNotification);
    assertThat(postNotification.getAction().getActionId(), equalTo(MESSAGE_PROCESSOR_POST_INVOKE));
    assertThat(postNotification.getEventContext(), equalTo(inEvent.getContext()));
    assertThat(postNotification.getEvent(), not(equalTo(inEvent)));
    assertThat(postNotification.getEvent().getMessage().getPayload().getValue(), equalTo(resultPayload));
    assertThat(postNotification.getException(), is(nullValue()));
  }

  @Test
  public void testErrorNotifications() throws Exception {
    List<MessageProcessorNotification> notificationList = new ArrayList<>();
    setupMessageProcessorNotificationListener(notificationList);
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new ExceptionThrowingMessageProcessor(illegalStateException));
    final CoreEvent inEvent = getTestEventUsingFlow("0");
    try {
      process(builder.build(), inEvent);
    } catch (Throwable t) {
      assertThat(t, is(illegalStateException));
      assertThat(notificationList, hasSize(2));
      MessageProcessorNotification preNotification = notificationList.get(0);
      MessageProcessorNotification postNotification = notificationList.get(1);
      assertPreNotification(inEvent, preNotification);
      assertThat(postNotification.getAction().getActionId(), equalTo(MESSAGE_PROCESSOR_POST_INVOKE));
      assertThat(postNotification.getEventContext(), equalTo(inEvent.getContext()));
      assertPostErrorNotification(inEvent, postNotification);
    }
  }

  @Test
  public void testErrorNotificationsMessagingException() throws Exception {
    List<MessageProcessorNotification> notificationList = new ArrayList<>();
    setupMessageProcessorNotificationListener(notificationList);
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final CoreEvent messagingExceptionEvent = getTestEventUsingFlow("other");
    final MessagingException messagingException = new MessagingException(messagingExceptionEvent, illegalStateException);
    builder.chain(new ExceptionThrowingMessageProcessor(messagingException));
    final CoreEvent inEvent = getTestEventUsingFlow("0");
    try {
      process(builder.build(), inEvent);
    } catch (Throwable t) {
      assertThat(t, instanceOf(IllegalStateException.class));
      assertThat(notificationList, hasSize(2));
      MessageProcessorNotification preNotification = notificationList.get(0);
      MessageProcessorNotification postNotification = notificationList.get(1);
      assertPreNotification(inEvent, preNotification);
      assertThat(postNotification.getAction().getActionId(), equalTo(MESSAGE_PROCESSOR_POST_INVOKE));
      // Existing MessagingException.event is updated to include error
      assertThat(postNotification.getEvent(), not(messagingExceptionEvent));
      assertPostErrorNotification(inEvent, postNotification);
    }
  }

  @Test
  public void subscriptionContextPropagation() throws Exception {
    final ProcessingStrategy processingStrategy = processingStrategyFactory.create(muleContext, "");
    initialiseIfNeeded(processingStrategy);
    startIfNeeded(processingStrategy);

    try {
      final MessageProcessorChain innerChain = newChain(Optional.of(processingStrategy), p -> p);
      final MessageProcessorChain outerChain = newChain(Optional.of(processingStrategy), new Processor() {

        @Override
        public CoreEvent process(CoreEvent event) throws MuleException {
          return processToApply(event, this);
        }

        @Override
        public Publisher<CoreEvent> apply(Publisher<CoreEvent> p) {
          return Flux.from(p)
              .subscriberContext(ctx -> {
                ctx.get("key");
                return ctx;
              })
              .transform(innerChain)
              .subscriberContext(ctx -> {
                ctx.get("key");
                return ctx;
              });
        };
      });

      if (innerChain instanceof MuleContextAware) {
        ((MuleContextAware) innerChain).setMuleContext(muleContext);
      }
      if (outerChain instanceof MuleContextAware) {
        ((MuleContextAware) outerChain).setMuleContext(muleContext);
      }

      Processor caller = new Processor() {

        @Override
        public CoreEvent process(CoreEvent event) throws MuleException {
          return processToApply(event, this);
        }

        @Override
        public org.reactivestreams.Publisher<CoreEvent> apply(org.reactivestreams.Publisher<CoreEvent> p) {
          return Flux.from(p)
              .transform(outerChain)
              .subscriberContext(ctx -> ctx.put("key", "value"));
        };
      };

      process(caller, testEvent());
    } finally {
      stopIfNeeded(processingStrategy);
      disposeIfNeeded(processingStrategy, LOGGER);
    }
  }

  private void setupMessageProcessorNotificationListener(List<MessageProcessorNotification> notificationList) {
    muleContext.getNotificationManager().addInterfaceToType(MessageProcessorNotificationListener.class,
                                                            MessageProcessorNotification.class);
    muleContext.getNotificationManager().addListener((MessageProcessorNotificationListener) notification -> {
      notificationList.add((MessageProcessorNotification) notification);
    });
  }

  private void assertPreNotification(CoreEvent inEvent, MessageProcessorNotification preNotification) {
    assertThat(preNotification.getAction().getActionId(), equalTo(MESSAGE_PROCESSOR_PRE_INVOKE));
    assertThat(preNotification.getEventContext(), equalTo(inEvent.getContext()));
    assertThat(preNotification.getEvent(), equalTo(inEvent));
    assertThat(preNotification.getException(), is(nullValue()));
  }

  private void assertPostErrorNotification(CoreEvent inEvent, MessageProcessorNotification postNotification) {
    assertThat(postNotification.getEvent(), not(equalTo(inEvent)));
    assertThat(postNotification.getEvent().getError().isPresent(), is(true));
    assertThat(postNotification.getEvent().getError().get().getCause(), is(illegalStateException));
    assertThat(postNotification.getException(), is(instanceOf(MessagingException.class)));
    assertThat(postNotification.getException().getCause(), is(illegalStateException));
  }

  @Override
  protected CoreEvent process(Processor messageProcessor, CoreEvent event) throws Exception {
    initialiseIfNeeded(messageProcessor, muleContext);
    startIfNeeded(messageProcessor);

    try {
      return super.process(messageProcessor, event);
    } finally {
      final SchedulerService schedulerService = muleContext.getSchedulerService();
    }
  }

  private AppendingMP getAppendingMP(String append) {
    return new NonBlockingAppendingMP(append);
  }

  static class TestNonIntercepting implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return CoreEvent.builder(event).message(of(event.getMessage().getPayload().getValue() + "MessageProcessor")).build();
    }
  }

  static class TestIntercepting extends AbstractInterceptingMessageProcessor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return processNext(CoreEvent.builder(event)
          .message(of(event.getMessage().getPayload().getValue() + "InterceptingMessageProcessor")).build());
    }
  }

  private void assertLifecycle(AppendingInterceptingMP mp) {
    assertTrue(mp.muleContextInjected);
    assertTrue(mp.initialised);
    assertTrue(mp.started);
    assertTrue(mp.stopped);
    assertTrue(mp.disposed);
  }

  class NonBlockingAppendingMP extends AppendingMP {

    /**
     * Force the proactor to change the thread.
     */
    @Override
    public ProcessingType getProcessingType() {
      return CPU_INTENSIVE;
    }

    public NonBlockingAppendingMP(String append) {
      super(append);
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      nonBlockingProcessorsExecuted.incrementAndGet();
      return super.process(event);
    }
  }

  class AppendingMP extends AbstractComponent implements Processor, Lifecycle, MuleContextAware {

    String appendString;
    boolean muleContextInjected;
    boolean flowConstuctInjected;
    boolean initialised;
    boolean started;
    boolean stopped;
    boolean disposed;
    CoreEvent event;
    CoreEvent resultEvent;

    public AppendingMP(String append) {
      this.appendString = append;
    }

    @Override
    public CoreEvent process(final CoreEvent event) throws MuleException {
      return innerProcess(event);
    }

    private CoreEvent innerProcess(CoreEvent event) {
      this.event = event;
      CoreEvent result =
          CoreEvent.builder(event).message(of(event.getMessage().getPayload().getValue() + appendString)).build();
      this.resultEvent = result;
      return result;
    }

    @Override
    public void initialise() throws InitialisationException {
      initialised = true;
    }

    @Override
    public void start() throws MuleException {
      started = true;
    }

    @Override
    public void stop() throws MuleException {
      stopped = true;
    }

    @Override
    public void dispose() {
      disposed = true;
    }

    @Override
    public String toString() {
      return ObjectUtils.toString(this);
    }

    @Override
    public void setMuleContext(MuleContext context) {
      this.muleContextInjected = true;
    }

    @Override
    public ComponentLocation getLocation() {
      return mock(ComponentLocation.class);
    }
  }

  class AppendingInterceptingMP extends AbstractInterceptingMessageProcessor implements Lifecycle {

    String appendString;
    boolean muleContextInjected;
    boolean initialised;
    boolean started;
    boolean stopped;
    boolean disposed;
    private final boolean stopProcessing;
    boolean invoked;

    public AppendingInterceptingMP(String appendString) {
      this(appendString, false);
    }

    public AppendingInterceptingMP(String appendString, boolean stopProcessing) {
      this.appendString = appendString;
      this.stopProcessing = stopProcessing;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return processToApply(event, this);
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      if (stopProcessing) {
        return publisher;
      } else {
        return from(publisher).map(before -> appendBefore(before)).transform(applyNext())
            .map(after -> {
              if (after != null) {
                return appendAfter(after);
              } else {
                return after;
              }
            });
      }
    }

    private CoreEvent appendAfter(CoreEvent after) {
      return CoreEvent.builder(after).message(of(after.getMessage().getPayload().getValue() + "after" + appendString))
          .build();
    }

    private CoreEvent appendBefore(CoreEvent before) {
      return CoreEvent.builder(before).message(of(before.getMessage().getPayload().getValue() + "before" + appendString))
          .build();
    }

    @Override
    public void initialise() throws InitialisationException {
      initialised = true;
    }

    @Override
    public void start() throws MuleException {
      started = true;
    }

    @Override
    public void stop() throws MuleException {
      stopped = true;
    }

    @Override
    public void dispose() {
      disposed = true;
    }

    @Override
    public String toString() {
      return ObjectUtils.toString(this);
    }

    @Override
    public void setMuleContext(MuleContext context) {
      this.muleContextInjected = true;
      super.setMuleContext(context);
    }

  }

  static class ReturnNullMP implements Processor {

    CoreEvent event;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      this.event = event;
      return null;
    }
  }

  static class ReturnNullInterceptongMP extends AbstractInterceptingMessageProcessor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return null;
    }
  }

  private static class ReturnVoidMP implements Processor {

    CoreEvent event;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      this.event = event;
      return event;
    }
  }

  static class ReturnVoidMPInterceptongMP extends AbstractInterceptingMessageProcessor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return event;
    }
  }

  protected CoreEvent getTestEventUsingFlow(Object data) throws MuleException {
    return CoreEvent.builder(create(flow, TEST_CONNECTOR_LOCATION)).message(of(data)).build();
  }

  public static class ExceptionThrowingMessageProcessor extends AbstractComponent implements Processor, InternalProcessor {

    private final Exception exception;

    public ExceptionThrowingMessageProcessor(Exception exception) {
      this.exception = exception;
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      if (exception instanceof MuleException) {
        throw (MuleException) exception;
      } else if (exception instanceof RuntimeException) {
        throw (RuntimeException) exception;
      } else {
        throw new MuleRuntimeException(exception);
      }
    }

    @Override
    public ComponentLocation getLocation() {
      return mock(ComponentLocation.class);
    }

  }

}
