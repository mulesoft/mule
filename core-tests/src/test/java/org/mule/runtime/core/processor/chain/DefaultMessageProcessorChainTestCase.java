/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.chain;

import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.MessageExchangePattern.ONE_WAY;
import static org.mule.runtime.core.MessageExchangePattern.REQUEST_RESPONSE;
import static org.mule.runtime.core.api.processor.MessageProcessors.newChain;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.DefaultEventContext;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.MessageProcessors;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.context.notification.DefaultFlowCallStack;
import org.mule.runtime.core.exception.ErrorTypeLocator;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.routing.ChoiceRouter;
import org.mule.runtime.core.routing.ScatterGatherRouter;
import org.mule.runtime.core.routing.filters.AcceptAllFilter;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
@SmallTest
@SuppressWarnings("deprecation")
public class DefaultMessageProcessorChainTestCase extends AbstractMuleContextTestCase {

  protected MuleContext muleContext;

  protected MessageExchangePattern exchangePattern;
  protected boolean nonBlocking;
  protected boolean synchronous;
  private volatile int threads = 1;

  private ExecutorService executor = Executors.newCachedThreadPool();

  @Parameterized.Parameters
  public static Collection<Object[]> parameters() {
    return Arrays.asList(new Object[][] {
        {REQUEST_RESPONSE, false, true},
        {REQUEST_RESPONSE, false, false},
        {REQUEST_RESPONSE, true, true},
        {REQUEST_RESPONSE, true, false},
        {ONE_WAY, false, true},
        {ONE_WAY, false, false},
        {ONE_WAY, true, true}});
  }

  private Pipeline mockFlow = mock(Flow.class);

  public DefaultMessageProcessorChainTestCase(MessageExchangePattern exchangePattern, boolean nonBlocking, boolean synchronous) {
    this.exchangePattern = exchangePattern;
    this.nonBlocking = nonBlocking;
    this.synchronous = synchronous;
  }

  @Before
  public void before() {
    muleContext = mock(MuleContext.class);
    ErrorTypeLocator errorTypeLocator = mock(ErrorTypeLocator.class);
    ErrorType errorType = mock(ErrorType.class);
    MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
    when(muleConfiguration.isContainerMode()).thenReturn(false);
    when(muleConfiguration.getId()).thenReturn(RandomStringUtils.randomNumeric(3));
    when(muleConfiguration.getShutdownTimeout()).thenReturn(1000);
    when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    when(muleContext.getErrorTypeLocator()).thenReturn(errorTypeLocator);
    when(errorTypeLocator.lookupErrorType(any())).thenReturn(errorType);
  }

  @After
  public void after() {
    executor.shutdown();
  }

  @Test
  public void testMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"));
    assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
  }

  /*
   * Any MP returns null: - Processing doesn't proceed - Result of chain is Nnll
   */
  @Test
  public void testMPChainWithNullReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

    AppendingMP mp1 = getAppendingMP("1");
    AppendingMP mp2 = getAppendingMP("2");
    ReturnNullMP nullmp = new ReturnNullMP();
    AppendingMP mp3 = getAppendingMP("3");
    builder.chain(mp1, mp2, nullmp, mp3);

    Event requestEvent = getTestEventUsingFlow("0");
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

    assertEquals(isMultipleThreadsUsed() ? 3 : 1, threads);
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

    Event requestEvent = getTestEventUsingFlow("0");
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

    assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
  }

  @Test
  public void testMPChainWithNullReturnAtEnd() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnNullMP());
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));

    assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
  }

  @Test
  public void testMPChainWithVoidReturnAtEnd() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnVoidMP());
    assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
  }

  @Test
  public void testMPChainWithBuilder() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"));
    builder.chain((MessageProcessorBuilder) () -> getAppendingMP("2"));
    builder.chain(getAppendingMP("3"));
    assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
  }

  @Test
  public void testInterceptingMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"), new AppendingInterceptingMP("3"));
    assertEquals("0before1before2before3after3after2after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  public void testInterceptingMPChainWithNullReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

    AppendingInterceptingMP lastMP = new AppendingInterceptingMP("3");

    builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"), new ReturnNullInterceptongMP(), lastMP);
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    assertFalse(lastMP.invoked);

    assertEquals(1, threads);
  }

  @Test
  public void testInterceptingMPChainWithVoidReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

    AppendingInterceptingMP lastMP = new AppendingInterceptingMP("3");

    builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"), new ReturnNullInterceptongMP(), lastMP);
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));
    assertFalse(lastMP.invoked);

    assertEquals(1, threads);
  }

  @Test
  public void testMixedMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                  getAppendingMP("5"));
    assertEquals("0before123before45after4after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  // Whenever there is a IMP that returns null the final result is null
  public void testMixedMPChainWithNullReturn1() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new ReturnNullInterceptongMP(), getAppendingMP("2"), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));

    assertEquals(1, threads);
  }

  @Test
  // Whenever there is a IMP that returns null the final result is null
  public void testMixedMPChainWithVoidReturn1() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new ReturnVoidMPInterceptongMP(), getAppendingMP("2"), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue(),
               equalTo("0before1after1"));

    assertThat(threads, equalTo(1));
  }

  @Test
  // Whenever there is a IMP that returns null the final result is null
  public void testMixedMPChainWithNullReturn2() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnNullInterceptongMP(), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));

    assertEquals(1, threads);
  }

  @Test
  // Whenever there is a IMP that returns null the final result is null
  public void testMixedMPChainWithVoidlReturn2() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnVoidMPInterceptongMP(), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertEquals("0before12after1", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  // A simple MP that returns null does not affect flow as long as it's not at the
  // end
  public void testMixedMPChainWithNullReturn3() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new ReturnNullMP(), getAppendingMP("2"), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));

    assertEquals(1, threads);
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

    assertEquals(1, threads);
  }

  @Test
  // A simple MP that returns null does not affect flow as long as it's not at the
  // end
  public void testMixedMPChainWithNullReturn4() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnNullMP(), getAppendingMP("3"),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));

    assertEquals(1, threads);
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

    assertEquals(1, threads);
  }

  @Test
  // A simple MP that returns null does not affect flow as long as it's not at the
  // end
  public void testMixedMPChainWithNullReturn5() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnNullMP(),
                  new AppendingInterceptingMP("4"), getAppendingMP("5"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));

    assertEquals(1, threads);
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

    assertEquals(1, threads);
  }

  @Test
  // A simple MP at the end of a single level chain causes chain to return null
  public void testMixedMPChainWithNullReturnAtEnd() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                  getAppendingMP("5"), new ReturnNullMP());
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));

    assertEquals(1, threads);
  }

  @Test
  // A simple MP at the end of a single level chain causes chain to return null
  public void testMixedMPChainWithVoidReturnAtEnd() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                  getAppendingMP("5"), new ReturnVoidMP());
    assertEquals("0before123before45after4after1",
                 process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  public void testNestedMPChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"),
                  new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b")).build(),
                  getAppendingMP("2"));
    assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(isMultipleThreadsUsed() ? 5 : 1, threads);
  }

  @Test
  public void testNestedMPChainWithNullReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(
                  getAppendingMP("1"), new DefaultMessageProcessorChainBuilder()
                      .chain(getAppendingMP("a"), new ReturnNullMP(), getAppendingMP("b")).build(),
                  new ReturnNullMP(), getAppendingMP("2"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));

    assertEquals(isMultipleThreadsUsed() ? 3 : 1, threads);
  }

  @Test
  public void testNestedMPChainWithVoidReturn() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(
                  getAppendingMP("1"), new DefaultMessageProcessorChainBuilder()
                      .chain(getAppendingMP("a"), new ReturnVoidMP(), getAppendingMP("b")).build(),
                  new ReturnVoidMP(), getAppendingMP("2"));
    assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(isMultipleThreadsUsed() ? 5 : 1, threads);
  }

  @Test
  public void testNestedMPChainWithNullReturnAtEndOfNestedChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), new DefaultMessageProcessorChainBuilder()
        .chain(getAppendingMP("a"), getAppendingMP("b"), new ReturnNullMP()).build(), getAppendingMP("2"));
    assertNull(process(builder.build(), getTestEventUsingFlow("0")));

    assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
  }

  @Test
  public void testNestedMPChainWithVoidReturnAtEndOfNestedChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), new DefaultMessageProcessorChainBuilder()
        .chain(getAppendingMP("a"), getAppendingMP("b"), new ReturnVoidMP()).build(), getAppendingMP("2"));
    assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(isMultipleThreadsUsed() ? 5 : 1, threads);
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

    assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
  }

  @Test
  public void testNestedMPChainWithVoidReturnAtEndOfNestedChainWithNonInterceptingWrapper() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final MessageProcessorChain nested =
        new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b"), new ReturnVoidMP())
            .build();
    nested.setMuleContext(muleContext);
    builder.chain(getAppendingMP("1"), event -> nested
        .process(Event.builder(DefaultEventContext.create(mockFlow, TEST_CONNECTOR))
            .message(event.getMessage()).exchangePattern(REQUEST_RESPONSE).flow(mockFlow).build()),
                  getAppendingMP("2"));
    assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(isMultipleThreadsUsed() ? 3 : 1, threads);
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

    assertEquals(1, threads);
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

    assertEquals(1, threads);
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

    assertThat(threads, equalTo(1));
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

    assertEquals(isMultipleThreadsUsed() ? 2 : 1, threads);
  }

  @Test
  public void testInterceptingMPChainStopFlow() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2", true), new AppendingInterceptingMP("3"));
    assertEquals("0before1after1", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue());

    assertEquals(1, threads);
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

    assertEquals(1, threads);
  }

  @Test
  public void testMPChainLifecycle() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    AppendingInterceptingMP mp1 = new AppendingInterceptingMP("1");
    AppendingInterceptingMP mp2 = new AppendingInterceptingMP("2");
    Processor chain = builder.chain(mp1, mp2).build();
    ((MuleContextAware) chain).setMuleContext(mock(MuleContext.class, Mockito.RETURNS_DEEP_STUBS));
    ((FlowConstructAware) chain).setFlowConstruct(mock(FlowConstruct.class));
    ((Lifecycle) chain).initialise();
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
    ((MuleContextAware) chain).setMuleContext(mock(MuleContext.class, Mockito.RETURNS_DEEP_STUBS));
    ((FlowConstructAware) chain).setFlowConstruct(mock(FlowConstruct.class));
    ((Lifecycle) chain).initialise();
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
    Event restul = process(builder.build(), getTestEventUsingFlow(""));
    assertEquals("MessageProcessorMessageProcessorMessageProcessor", restul.getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  public void testAllIntercepting() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new TestIntercepting(), new TestIntercepting(), new TestIntercepting());
    Event restul = process(builder.build(), getTestEventUsingFlow(""));
    assertEquals("InterceptingMessageProcessorInterceptingMessageProcessorInterceptingMessageProcessor",
                 restul.getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  public void testMix() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(), new TestIntercepting(),
                  new TestNonIntercepting(), new TestNonIntercepting());
    Event restul = process(builder.build(), getTestEventUsingFlow(""));
    assertEquals("InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
                 restul.getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  public void testMixStaticFactoryt() throws Exception {
    MessageProcessorChain chain =
        MessageProcessors.newChain(new TestIntercepting(), new TestNonIntercepting(),
                                   new TestNonIntercepting(), new TestIntercepting(), new TestNonIntercepting(),
                                   new TestNonIntercepting());
    Event restul = process(chain, getTestEventUsingFlow(""));
    assertEquals("InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
                 restul.getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  public void testMix2() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new TestNonIntercepting(), new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(),
                  new TestNonIntercepting(), new TestIntercepting());
    Event restul = process(builder.build(), getTestEventUsingFlow(""));
    assertEquals("MessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
                 restul.getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  public void testMix2StaticFactory() throws Exception {
    MessageProcessorChain chain =
        MessageProcessors.newChain(new TestNonIntercepting(), new TestIntercepting(),
                                   new TestNonIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(),
                                   new TestIntercepting());
    Event result = process(chain, getTestEventUsingFlow(""));
    assertEquals("MessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
                 result.getMessage().getPayload().getValue());

    assertEquals(1, threads);
  }

  @Test
  public void testResponseProcessor() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final ResponseMessageProcessorAdapter responseMessageProcessorAdapter =
        new ResponseMessageProcessorAdapter(getAppendingMP("3"));
    responseMessageProcessorAdapter.setMuleContext(muleContext);
    builder.chain(getAppendingMP("1"), responseMessageProcessorAdapter, getAppendingMP("2"));
    assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue(), equalTo("0123"));

    assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
  }

  @Test
  public void testResponseProcessorInNestedChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final ResponseMessageProcessorAdapter responseMessageProcessorAdapter =
        new ResponseMessageProcessorAdapter(getAppendingMP("c"));
    responseMessageProcessorAdapter.setMuleContext(muleContext);
    builder.chain(
                  getAppendingMP("1"), MessageProcessors.newChain(getAppendingMP("a"),
                                                                  responseMessageProcessorAdapter, getAppendingMP("b")),
                  getAppendingMP("2"));
    assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue(), equalTo("01abc2"));

    assertEquals(isMultipleThreadsUsed() ? 6 : 1, threads);
  }

  @Test
  public void testNestedResponseProcessor() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final ResponseMessageProcessorAdapter innerResponseMessageProcessorAdapter =
        new ResponseMessageProcessorAdapter(getAppendingMP("4"));
    final ResponseMessageProcessorAdapter responseMessageProcessorAdapter =
        new ResponseMessageProcessorAdapter(newChain(innerResponseMessageProcessorAdapter, getAppendingMP("3")));
    builder.chain(getAppendingMP("1"), responseMessageProcessorAdapter, getAppendingMP("2"));
    process(builder.build(), getTestEventUsingFlow("0"));
    assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue(), equalTo("01234"));
    assertEquals(isMultipleThreadsUsed() ? 9 : 1, threads);
  }

  @Test
  public void testNestedResponseProcessorEndOfChain() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    final MessageProcessorChain chain = newChain(singletonList(getAppendingMP("1")));
    final ResponseMessageProcessorAdapter responseMessageProcessorAdapter = new ResponseMessageProcessorAdapter(chain);
    responseMessageProcessorAdapter.setMuleContext(muleContext);
    builder.chain(responseMessageProcessorAdapter);
    process(builder.build(), getTestEventUsingFlow("0"));
    assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload().getValue(), equalTo("01"));
    assertEquals(isMultipleThreadsUsed() ? 3 : 1, threads);
  }

  @Test
  public void testAll() throws Exception {
    ScatterGatherRouter scatterGatherRouter = new ScatterGatherRouter();
    scatterGatherRouter.addRoute(getAppendingMP("1"));
    scatterGatherRouter.addRoute(getAppendingMP("2"));
    scatterGatherRouter.addRoute(getAppendingMP("3"));
    ThreadingProfile tp = ThreadingProfile.DEFAULT_THREADING_PROFILE;
    tp.setMuleContext(muleContext);
    scatterGatherRouter.setThreadingProfile(tp);
    scatterGatherRouter.setMuleContext(muleContext);
    scatterGatherRouter.initialise();
    scatterGatherRouter.start();

    Event event = getTestEventUsingFlow("0");
    final MessageProcessorChain chain = newChain(singletonList(scatterGatherRouter));
    InternalMessage result = process(chain, Event.builder(event).message(event.getMessage()).build()).getMessage();
    assertThat(result.getPayload().getValue(), instanceOf(List.class));
    List<InternalMessage> resultMessage = (List<InternalMessage>) result.getPayload().getValue();
    assertThat(resultMessage.stream().map(msg -> msg.getPayload().getValue()).collect(toList()).toArray(),
               is(equalTo(new String[] {"01", "02", "03"})));
    assertEquals(1, threads);

    scatterGatherRouter.stop();
    scatterGatherRouter.dispose();
  }

  @Test
  public void testChoice() throws Exception {
    ChoiceRouter choiceRouter = new ChoiceRouter();
    choiceRouter.addRoute(getAppendingMP("1"), new AcceptAllFilter());
    choiceRouter.addRoute(getAppendingMP("2"), new AcceptAllFilter());
    choiceRouter.addRoute(getAppendingMP("3"), new AcceptAllFilter());

    assertThat(process(newChain(choiceRouter), getTestEventUsingFlow("0")).getMessage().getPayload().getValue(), equalTo("01"));

    assertEquals(isMultipleThreadsUsed() ? 2 : 1, threads);
  }

  @Test(expected = MessagingException.class)
  public void testExceptionAfter() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), new ExceptionThrowingMessageProcessor());
    process(builder.build(), getTestEventUsingFlow("0"));
  }

  @Test(expected = MessagingException.class)
  public void testExceptionBefore() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new ExceptionThrowingMessageProcessor(), getAppendingMP("1"));
    process(builder.build(), getTestEventUsingFlow("0"));
  }

  @Test(expected = MessagingException.class)
  public void testExceptionBetween() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(getAppendingMP("1"), new ExceptionThrowingMessageProcessor(), getAppendingMP("2"));
    process(builder.build(), getTestEventUsingFlow("0"));
  }

  @Test(expected = MessagingException.class)
  public void testExceptionInResponse() throws Exception {
    DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
    builder.chain(new ResponseMessageProcessorAdapter(new ExceptionThrowingMessageProcessor()), getAppendingMP("1"));
    process(builder.build(), getTestEventUsingFlow("0"));
  }

  private Event process(MessageProcessorChain messageProcessorChain, Event event) throws Exception {
    messageProcessorChain.setFlowConstruct(mockFlow);
    messageProcessorChain.setMuleContext(muleContext);
    Event result;
    if (nonBlocking && exchangePattern.hasResponse()) {
      SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
      event = Event.builder(event).replyToHandler(nullReplyToHandler).build();
      result = messageProcessorChain.process(event);
      if (NonBlockingVoidMuleEvent.getInstance() == result) {
        nullReplyToHandler.latch.await(1000, TimeUnit.MILLISECONDS);
        if (nullReplyToHandler.exception != null) {
          throw nullReplyToHandler.exception;
        } else {
          result = nullReplyToHandler.event;
        }
      }
    } else {
      result = messageProcessorChain.process(event);
    }
    return result;
  }

  private AppendingMP getAppendingMP(String append) {
    if (nonBlocking) {
      return new NonBlockingAppendingMP(append);
    } else {
      return new AppendingMP(append);
    }
  }

  private boolean isMultipleThreadsUsed() {
    return nonBlocking && exchangePattern.hasResponse() && !synchronous;
  }

  static class TestNonIntercepting implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      return Event.builder(event)
          .message(InternalMessage.builder().payload(event.getMessage().getPayload().getValue() + "MessageProcessor").build())
          .build();
    }
  }

  static class TestIntercepting extends AbstractInterceptingMessageProcessor {

    @Override
    public Event process(Event event) throws MuleException {
      return processNext(Event.builder(event)
          .message(InternalMessage.builder().payload(event.getMessage().getPayload().getValue() + "InterceptingMessageProcessor")
              .build())
          .build());
    }
  }

  private void assertLifecycle(AppendingMP mp) {
    assertTrue(mp.flowConstuctInjected);
    assertTrue(mp.muleContextInjected);
    assertTrue(mp.initialised);
    assertTrue(mp.started);
    assertTrue(mp.stopped);
    assertTrue(mp.disposed);
  }

  private void assertLifecycle(AppendingInterceptingMP mp) {
    assertTrue(mp.flowConstuctInjected);
    assertTrue(mp.muleContextInjected);
    assertTrue(mp.initialised);
    assertTrue(mp.started);
    assertTrue(mp.stopped);
    assertTrue(mp.disposed);
  }

  class NonBlockingAppendingMP extends AppendingMP implements NonBlockingMessageProcessor {

    public NonBlockingAppendingMP(String append) {
      super(append);
    }
  }

  class AppendingMP implements Processor, Lifecycle, FlowConstructAware, MuleContextAware {

    String appendString;
    boolean muleContextInjected;
    boolean flowConstuctInjected;
    boolean initialised;
    boolean started;
    boolean stopped;
    boolean disposed;
    Event event;
    Event resultEvent;

    public AppendingMP(String append) {
      this.appendString = append;
    }

    @Override
    public Event process(final Event event) throws MuleException {
      if (nonBlocking && event.isAllowNonBlocking() && event.getReplyToHandler() != null) {
        executor.execute(() -> {
          try {
            threads++;
            event.getReplyToHandler().processReplyTo(innerProcess(event), null, null);
          } catch (MessagingException e1) {
            event.getReplyToHandler().processExceptionReplyTo(e1, null);
          } catch (MuleException e2) {
            e2.printStackTrace();
          }
        });
        return NonBlockingVoidMuleEvent.getInstance();

      } else {
        return innerProcess(event);
      }
    }

    private Event innerProcess(Event event) {
      this.event = event;
      Event result = Event.builder(event)
          .message(InternalMessage.builder().payload(event.getMessage().getPayload().getValue() + appendString).build()).build();
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
    public void setFlowConstruct(FlowConstruct flowConstruct) {
      this.flowConstuctInjected = true;
    }
  }

  class AppendingInterceptingMP extends AbstractInterceptingMessageProcessor implements FlowConstructAware, Lifecycle {

    String appendString;
    boolean muleContextInjected;
    boolean flowConstuctInjected;
    boolean initialised;
    boolean started;
    boolean stopped;
    boolean disposed;
    Event event;
    Event resultEvent;
    private boolean stopProcessing;
    boolean invoked = false;

    public AppendingInterceptingMP(String appendString) {
      this(appendString, false);
    }

    public AppendingInterceptingMP(String appendString, boolean stopProcessing) {
      this.appendString = appendString;
      this.stopProcessing = stopProcessing;
    }

    @Override
    public Event process(Event event) throws MuleException {
      if (stopProcessing) {
        return event;
      }

      Event intermediateEvent = Event.builder(event)
          .message(InternalMessage.builder().payload(event.getMessage().getPayload().getValue() + "before" + appendString)
              .build())
          .build();
      Event result = processNext(intermediateEvent);
      if (result != null && !result.equals(VoidMuleEvent.getInstance())
          && !result.equals(NonBlockingVoidMuleEvent.getInstance())) {
        return Event.builder(result)
            .message(InternalMessage.builder().payload(result.getMessage().getPayload().getValue() + "after" + appendString)
                .build())
            .build();
      } else {
        return result;
      }

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
    public void setFlowConstruct(FlowConstruct flowConstruct) {
      this.flowConstuctInjected = true;
    }
  }

  static class ReturnNullMP implements Processor {

    Event event;

    @Override
    public Event process(Event event) throws MuleException {
      this.event = event;
      return null;
    }
  }

  static class ReturnNullInterceptongMP extends AbstractInterceptingMessageProcessor {

    @Override
    public Event process(Event event) throws MuleException {
      return null;
    }
  }

  private static class ReturnVoidMP implements Processor {

    Event event;

    @Override
    public Event process(Event event) throws MuleException {
      this.event = event;
      return VoidMuleEvent.getInstance();
    }
  }

  static class ReturnVoidMPInterceptongMP extends AbstractInterceptingMessageProcessor {

    @Override
    public Event process(Event event) throws MuleException {
      return VoidMuleEvent.getInstance();
    }
  }

  protected Event getTestEventUsingFlow(Object data) throws Exception {
    Event event = mock(Event.class);
    InternalMessage message = InternalMessage.builder().payload(data).build();
    when(event.getFlowCallStack()).thenReturn(new DefaultFlowCallStack());
    when(event.getMessage()).thenReturn(message);
    when(event.getExchangePattern()).thenReturn(exchangePattern);
    when(mockFlow.getProcessingStrategy())
        .thenReturn(nonBlocking ? new NonBlockingProcessingStrategy() : new DefaultFlowProcessingStrategy());
    when(mockFlow.getMuleContext()).thenReturn(muleContext);
    when(event.getSession()).thenReturn(mock(MuleSession.class));
    when(event.isSynchronous()).thenReturn(synchronous);
    when(event.isAllowNonBlocking()).thenReturn(!synchronous && nonBlocking);
    when(event.getError()).thenReturn(empty());
    // when(event.isNonBlocking()).thenReturn(nonBlocking);
    return event;
  }

  public static class ExceptionThrowingMessageProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      throw new IllegalStateException();
    }
  }

}
