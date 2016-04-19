/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.processor.chain;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
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
import static org.mockito.Mockito.withSettings;

import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.endpoint.OutboundEndpoint;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorBuilder;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.runtime.core.processor.AbstractInterceptingMessageProcessor;
import org.mule.runtime.core.processor.AbstractMessageProcessorTestCase;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.processor.ResponseMessageProcessorAdapter;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.routing.ChoiceRouter;
import org.mule.runtime.core.routing.ScatterGatherRouter;
import org.mule.runtime.core.routing.filters.AcceptAllFilter;
import org.mule.runtime.core.util.ObjectUtils;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
@SmallTest
@SuppressWarnings("deprecation")
public class DefaultMessageProcessorChainTestCase extends AbstractMuleTestCase
{

    protected MuleContext muleContext;

    protected MessageExchangePattern exchangePattern;
    protected boolean nonBlocking;
    protected boolean synchronous;
    private volatile int threads = 1;

    private Executor executor = Executors.newCachedThreadPool();

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {MessageExchangePattern.REQUEST_RESPONSE, false, true},
                {MessageExchangePattern.REQUEST_RESPONSE, false, false},
                {MessageExchangePattern.REQUEST_RESPONSE, true, true},
                {MessageExchangePattern.REQUEST_RESPONSE, true, false},
                {MessageExchangePattern.ONE_WAY, false, true},
                {MessageExchangePattern.ONE_WAY, false, false},
                {MessageExchangePattern.ONE_WAY, true, true}
        });
    }

    public DefaultMessageProcessorChainTestCase(MessageExchangePattern exchangePattern, boolean nonBlocking, boolean
            synchronous)
    {
        this.exchangePattern = exchangePattern;
        this.nonBlocking = nonBlocking;
        this.synchronous = synchronous;
    }

    @Before
    public void before()
    {
        muleContext = mock(MuleContext.class);
        MuleConfiguration muleConfiguration = mock(MuleConfiguration.class);
        when(muleConfiguration.isContainerMode()).thenReturn(false);
        when(muleConfiguration.getId()).thenReturn(RandomStringUtils.randomNumeric(3));
        when(muleConfiguration.getShutdownTimeout()).thenReturn(1000);
        when(muleContext.getConfiguration()).thenReturn(muleConfiguration);
    }

    @Test
    public void testMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"));
        assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
    }

    /*
     * Any MP returns null: - Processing doesn't proceed - Result of chain is Nnll
     */
    @Test
    public void testMPChainWithNullReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

        AppendingMP mp1 = getAppendingMP("1");
        AppendingMP mp2 = getAppendingMP("2");
        ReturnNullMP nullmp = new ReturnNullMP();
        AppendingMP mp3 = getAppendingMP("3");
        builder.chain(mp1, mp2, nullmp, mp3);

        MuleEvent requestEvent = getTestEventUsingFlow("0");
        assertNull(process(builder.build(), requestEvent));

        // mp1
        assertSame(requestEvent.getMessage(), mp1.event.getMessage());
        assertNotSame(mp1.event, mp1.resultEvent);
        assertEquals("01", mp1.resultEvent.getMessage().getPayload());

        // mp2
        assertSame(mp1.resultEvent.getMessage(), mp2.event.getMessage());
        assertNotSame(mp2.event, mp2.resultEvent);
        assertEquals("012", mp2.resultEvent.getMessage().getPayload());

        // nullmp
        assertSame(mp2.resultEvent.getMessage(), nullmp.event.getMessage());
        assertEquals("012", nullmp.event.getMessage().getPayload());

        // mp3
        assertNull(mp3.event);

        assertEquals(isMultipleThreadsUsed() ? 3 : 1, threads);
    }

    /*
     * Any MP returns null: - Processing doesn't proceed - Result of chain is Nnll
     */
    @Test
    public void testMPChainWithVoidReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

        AppendingMP mp1 = getAppendingMP("1");
        AppendingMP mp2 = getAppendingMP("2");
        ReturnVoidMP voidmp = new ReturnVoidMP();
        AppendingMP mp3 = getAppendingMP("3");
        builder.chain(mp1, mp2, voidmp, mp3);

        MuleEvent requestEvent = getTestEventUsingFlow("0");
        assertEquals("0123", process(builder.build(), requestEvent).getMessage().getPayload());

        // mp1
        //assertSame(requestEvent, mp1.event);
        assertNotSame(mp1.event, mp1.resultEvent);

        // mp2
        //assertSame(mp1.resultEvent, mp2.event);
        assertNotSame(mp2.event, mp2.resultEvent);

        // void mp
        assertEquals(mp2.resultEvent, voidmp.event);

        // mp3
        assertNotSame(mp3.event, mp2.resultEvent);
        assertEquals(mp2.resultEvent.getMessage().getPayload(), mp3.event.getMessage().getPayload());
        assertEquals(mp3.event.getMessage().getPayload(), "012");

        assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
    }

    @Test
    public void testMPChainWithNullReturnAtEnd() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnNullMP());
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
    }

    @Test
    public void testMPChainWithVoidReturnAtEnd() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), getAppendingMP("2"), getAppendingMP("3"), new ReturnVoidMP());
        assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
    }

    @Test
    public void testMPChainWithBuilder() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"));
        builder.chain(new MessageProcessorBuilder()
        {
            @Override
            public MessageProcessor build()
            {
                return getAppendingMP("2");
            }
        });
        builder.chain(getAppendingMP("3"));
        assertEquals("0123", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
    }

    @Test
    public void testInterceptingMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"),
                      new AppendingInterceptingMP("3"));
        assertEquals("0before1before2before3after3after2after1",
                     process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testInterceptingMPChainWithNullReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

        AppendingInterceptingMP lastMP = new AppendingInterceptingMP("3");

        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"),
                      new ReturnNullInterceptongMP(), lastMP);
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
        assertFalse(lastMP.invoked);

        assertEquals(1, threads);
    }

    @Test
    public void testInterceptingMPChainWithVoidReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();

        AppendingInterceptingMP lastMP = new AppendingInterceptingMP("3");

        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2"),
                      new ReturnNullInterceptongMP(), lastMP);
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));
        assertFalse(lastMP.invoked);

        assertEquals(1, threads);
    }

    @Test
    public void testMixedMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertEquals("0before123before45after4after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(1, threads);
    }

    @Test
    // Whenever there is a IMP that returns null the final result is null
    public void testMixedMPChainWithNullReturn1() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new ReturnNullInterceptongMP(), getAppendingMP("2"),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(1, threads);
    }

    @Test
    // Whenever there is a IMP that returns null the final result is null
    public void testMixedMPChainWithVoidReturn1() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new ReturnVoidMPInterceptongMP(),
                      getAppendingMP("2"), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                      getAppendingMP("5"));
        assertSame("0", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(1, threads);
    }

    @Test
    // Whenever there is a IMP that returns null the final result is null
    public void testMixedMPChainWithNullReturn2() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnNullInterceptongMP(),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(1, threads);
    }

    @Test
    // Whenever there is a IMP that returns null the final result is null
    public void testMixedMPChainWithVoidlReturn2() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"),
                      new ReturnVoidMPInterceptongMP(), getAppendingMP("3"), new AppendingInterceptingMP("4"),
                      getAppendingMP("5"));
        assertEquals("0before12after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(1, threads);
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithNullReturn3() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new ReturnNullMP(), getAppendingMP("2"),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(1, threads);
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithVoidReturn3() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new ReturnVoidMP(), getAppendingMP("2"),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertEquals("0before123before45after4after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(1, threads);
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithNullReturn4() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnNullMP(),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(1, threads);
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithVoidReturn4() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), new ReturnVoidMP(),
                      getAppendingMP("3"), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertEquals("0before123before45after4after1", process(builder.build(),
                                                               getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(1, threads);
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithNullReturn5() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new ReturnNullMP(), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(1, threads);
    }

    @Test
    // A simple MP that returns null does not affect flow as long as it's not at the
    // end
    public void testMixedMPChainWithVoidReturn5() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new ReturnVoidMP(), new AppendingInterceptingMP("4"), getAppendingMP("5"));
        assertEquals("0before123before45after4after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(1, threads);
    }

    @Test
    // A simple MP at the end of a single level chain causes chain to return null
    public void testMixedMPChainWithNullReturnAtEnd() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new AppendingInterceptingMP("4"), getAppendingMP("5"), new ReturnNullMP());
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(1, threads);
    }

    @Test
    // A simple MP at the end of a single level chain causes chain to return null
    public void testMixedMPChainWithVoidReturnAtEnd() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), getAppendingMP("2"), getAppendingMP("3"),
                      new AppendingInterceptingMP("4"), getAppendingMP("5"), new ReturnVoidMP());
        assertEquals("0before123before45after4after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testNestedMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"),
                      new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b"))
                              .build(), getAppendingMP("2"));
        assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(isMultipleThreadsUsed() ? 5 : 1, threads);
    }

    @Test
    public void testNestedMPChainWithNullReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), new ReturnNullMP(),
                                                                getAppendingMP("b")).build(), new ReturnNullMP(),
                getAppendingMP("2"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(isMultipleThreadsUsed() ? 3 : 1, threads);
    }

    @Test
    public void testNestedMPChainWithVoidReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), new ReturnVoidMP(),
                                                                getAppendingMP("b")).build(), new ReturnVoidMP(),
                getAppendingMP("2"));
        assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(isMultipleThreadsUsed() ? 5 : 1, threads);
    }

    @Test
    public void testNestedMPChainWithNullReturnAtEndOfNestedChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b"),
                                                                new ReturnNullMP()).build(), getAppendingMP("2"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
    }

    @Test
    public void testNestedMPChainWithVoidReturnAtEndOfNestedChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"), getAppendingMP("b"),
                                                                new ReturnVoidMP()).build(), getAppendingMP("2"));
        assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(isMultipleThreadsUsed() ? 5 : 1, threads);
    }

    @Test
    public void testNestedMPChainWithNullReturnAtEndOfNestedChainWithNonInterceptingWrapper()
            throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        final MessageProcessor nested = new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"),
                                                                                        getAppendingMP("b"), new
                        ReturnNullMP()).build();
        builder.chain(getAppendingMP("1"), new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return nested.process(event);
            }
        }, getAppendingMP("2"));
        assertNull("012", process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
    }

    @Test
    public void testNestedMPChainWithVoidReturnAtEndOfNestedChainWithNonInterceptingWrapper()
            throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        final MessageProcessor nested = new DefaultMessageProcessorChainBuilder().chain(getAppendingMP("a"),
                                                                                        getAppendingMP("b"), new
                        ReturnVoidMP()).build();
        builder.chain(getAppendingMP("1"), new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return nested.process(new DefaultMuleEvent(event.getMessage(), MessageExchangePattern.REQUEST_RESPONSE, event.getFlowConstruct()));
            }
        }, getAppendingMP("2"));
        assertEquals("01ab2", process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(isMultipleThreadsUsed() ? 3 : 1, threads);
    }

    @Test
    public void testNestedInterceptingMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                new AppendingInterceptingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a"),
                                                                new AppendingInterceptingMP("b")).build(), new
                        AppendingInterceptingMP("2"));
        assertEquals("0before1beforeabeforebafterbafterabefore2after2after1",
                     process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testNestedInterceptingMPChainWithNullReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                new AppendingInterceptingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a"),
                                                                new ReturnNullInterceptongMP(), new
                                AppendingInterceptingMP("b")).build(),
                new AppendingInterceptingMP("2"));
        assertNull(process(builder.build(), getTestEventUsingFlow("0")));

        assertEquals(1, threads);
    }

    @Test
    public void testNestedInterceptingMPChainWithVoidReturn() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                new AppendingInterceptingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a"),
                                                                new ReturnVoidMPInterceptongMP(), new
                                AppendingInterceptingMP("b")).build(),
                new AppendingInterceptingMP("2"));
        assertEquals("0before1before2after2after1", process(builder.build()
                , getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testNestedMixedMPChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                getAppendingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a"),
                                                                getAppendingMP("b")).build(), new
                        AppendingInterceptingMP("2"));
        assertEquals("01beforeabafterabefore2after2", process(builder.build(), getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(isMultipleThreadsUsed() ? 2 : 1, threads);
    }

    @Test
    public void testInterceptingMPChainStopFlow() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AppendingInterceptingMP("1"), new AppendingInterceptingMP("2", true),
                      new AppendingInterceptingMP("3"));
        assertEquals("0before1after1", process(builder.build(),
                                               getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(1, threads);
    }

    /**
     * Note: Stopping the flow of a nested chain causes the nested chain to return
     * early, but does not stop the flow of the parent chain.
     */
    @Test
    public void testNestedInterceptingMPChainStopFlow() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(
                new AppendingInterceptingMP("1"),
                new DefaultMessageProcessorChainBuilder().chain(new AppendingInterceptingMP("a", true),
                                                                new AppendingInterceptingMP("b")).build(), new
                        AppendingInterceptingMP("3"));
        assertEquals("0before1before3after3after1", process(builder.build(),
                                                            getTestEventUsingFlow("0"))
                .getMessage()
                .getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testMPChainLifecycle() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        AppendingInterceptingMP mp1 = new AppendingInterceptingMP("1");
        AppendingInterceptingMP mp2 = new AppendingInterceptingMP("2");
        MessageProcessor chain = builder.chain(mp1, mp2).build();
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
    public void testNestedMPChainLifecycle() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        DefaultMessageProcessorChainBuilder nestedBuilder = new DefaultMessageProcessorChainBuilder();
        AppendingInterceptingMP mp1 = new AppendingInterceptingMP("1");
        AppendingInterceptingMP mp2 = new AppendingInterceptingMP("2");
        AppendingInterceptingMP mpa = new AppendingInterceptingMP("a");
        AppendingInterceptingMP mpb = new AppendingInterceptingMP("b");
        MessageProcessor chain = builder.chain(mp1, nestedBuilder.chain(mpa, mpb).build(), mp2).build();
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
    public void testNoneIntercepting() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestNonIntercepting(), new TestNonIntercepting(), new TestNonIntercepting());
        MuleEvent restul = process(builder.build(), getTestEventUsingFlow(""));
        assertEquals("MessageProcessorMessageProcessorMessageProcessor", restul.getMessage().getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testAllIntercepting() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestIntercepting(), new TestIntercepting(), new TestIntercepting());
        MuleEvent restul = process(builder.build(), getTestEventUsingFlow(""));
        assertEquals("InterceptingMessageProcessorInterceptingMessageProcessorInterceptingMessageProcessor",
                     restul.getMessage().getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testMix() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting(),
                      new TestIntercepting(), new TestNonIntercepting(), new TestNonIntercepting());
        MuleEvent restul = process(builder.build(), getTestEventUsingFlow(""));
        assertEquals(
                "InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
                restul.getMessage().getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testMixStaticFactoryt() throws Exception
    {
        MessageProcessorChain chain = DefaultMessageProcessorChain.from(new TestIntercepting(),
                                                                        new TestNonIntercepting(), new
                        TestNonIntercepting(), new TestIntercepting(),
                                                                        new TestNonIntercepting(), new
                                                                                TestNonIntercepting());
        MuleEvent restul = chain.process(getTestEventUsingFlow(""));
        assertEquals(
                "InterceptingMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessor",
                restul.getMessage().getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testMix2() throws Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new TestNonIntercepting(), new TestIntercepting(), new TestNonIntercepting(),
                      new TestNonIntercepting(), new TestNonIntercepting(), new TestIntercepting());
        MuleEvent restul = process(builder.build(), getTestEventUsingFlow(""));
        assertEquals(
                "MessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
                restul.getMessage().getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testMix2StaticFactory() throws Exception
    {
        MessageProcessorChain chain = DefaultMessageProcessorChain.from(new TestNonIntercepting(),
                                                                        new TestIntercepting(), new
                        TestNonIntercepting(), new TestNonIntercepting(),
                                                                        new TestNonIntercepting(), new
                                                                                TestIntercepting());
        MuleEvent restul = chain.process(getTestEventUsingFlow(""));
        assertEquals(
                "MessageProcessorInterceptingMessageProcessorMessageProcessorMessageProcessorMessageProcessorInterceptingMessageProcessor",
                restul.getMessage().getPayload());

        assertEquals(1, threads);
    }

    @Test
    public void testOneWayOutboundEndpointWithService() throws Exception
    {
        MuleEvent event = getTestEventUsingFlow("");
        when(event.getFlowConstruct()).thenReturn(mock(Flow.class));

        MessageProcessor mp = mock(MessageProcessor.class,
                withSettings().extraInterfaces(OutboundEndpoint.class));
        OutboundEndpoint outboundEndpoint = (OutboundEndpoint) mp;
        when(outboundEndpoint.getExchangePattern()).thenReturn(MessageExchangePattern.ONE_WAY);

        MessageProcessorChain chain = new DefaultMessageProcessorChainBuilder().chain(mp).build();
        MuleEvent response = chain.process(event);
        assertNull(response);

        assertEquals(1, threads);
    }

    @Test
    public void testOneWayOutboundEndpointWithFlow() throws Exception
    {
        MuleEvent event = getTestEventUsingFlow("");

        MessageProcessor mp = mock(MessageProcessor.class,
                withSettings().extraInterfaces(OutboundEndpoint.class));
        OutboundEndpoint outboundEndpoint = (OutboundEndpoint) mp;
        when(outboundEndpoint.getExchangePattern()).thenReturn(MessageExchangePattern.ONE_WAY);
        when(mp.process(any(MuleEvent.class))).thenReturn(VoidMuleEvent.getInstance());

        MessageProcessorChain chain = new DefaultMessageProcessorChainBuilder().chain(mp).build();
        MuleEvent response = chain.process(event);
        assertThat(event.getId(), is(response.getId()));
        assertThat(event.getMessage(), is(response.getMessage()));

        assertEquals(1, threads);
    }

    @Test
    public void testResponseProcessor() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), new ResponseMessageProcessorAdapter(getAppendingMP("3")),
                      getAppendingMP("2"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(), equalTo
                ("0123"));

        assertEquals(isMultipleThreadsUsed() ? 4 : 1, threads);
    }

    @Test
    public void testResponseProcessorInNestedChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), DefaultMessageProcessorChain.from
                              (getAppendingMP("a"), new ResponseMessageProcessorAdapter(getAppendingMP("c")),
                               getAppendingMP("b")),
                      getAppendingMP("2"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(), equalTo
                ("01abc2"));

        assertEquals(isMultipleThreadsUsed() ? 6 : 1, threads);
    }

    @Test
    public void testNestedResponseProcessor() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), new ResponseMessageProcessorAdapter(DefaultMessageProcessorChain.from
                              (new ResponseMessageProcessorAdapter(getAppendingMP("4")), getAppendingMP("3"))),
                      getAppendingMP("2"));
        process(builder.build(), getTestEventUsingFlow("0"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(), equalTo
                ("01234"));
        assertEquals(isMultipleThreadsUsed() ? 9 : 1, threads);
    }

    @Test
    public void testNestedResponseProcessorEndOfChain() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new ResponseMessageProcessorAdapter(DefaultMessageProcessorChain.from
                (getAppendingMP("1"))));
        process(builder.build(), getTestEventUsingFlow("0"));
        assertThat(process(builder.build(), getTestEventUsingFlow("0")).getMessage().getPayload(), equalTo
                ("01"));
        assertEquals(isMultipleThreadsUsed() ? 3 : 1, threads);
    }

    @Test
    public void testAll() throws MuleException, Exception
    {
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

        MuleEvent event = getTestEventUsingFlow("0");
        MuleMessage result = process(DefaultMessageProcessorChain.from(scatterGatherRouter), new DefaultMuleEvent
                (event.getMessage(), event)).getMessage();
        assertThat(result.getPayload(), instanceOf(List.class));
        List<MuleMessage> resultMessage = (List<MuleMessage>) result.getPayload();
        assertThat(resultMessage.stream().map(MuleMessage::getPayload).collect(Collectors.toList()).toArray(), Is.is(equalTo(new String[] {"01", "02", "03"})));
        assertEquals(1, threads);
    }

    @Test
    public void testChoice() throws MuleException, Exception
    {
        ChoiceRouter choiceRouter = new ChoiceRouter();
        choiceRouter.addRoute(getAppendingMP("1"), new AcceptAllFilter());
        choiceRouter.addRoute(getAppendingMP("2"), new AcceptAllFilter());
        choiceRouter.addRoute(getAppendingMP("3"), new AcceptAllFilter());

        assertThat(process(DefaultMessageProcessorChain.from(choiceRouter), getTestEventUsingFlow("0"))
                           .getMessage().getPayload(), equalTo("01"));

        assertEquals(isMultipleThreadsUsed() ? 2 : 1, threads);
    }

    @Test(expected = MessagingException.class)
    public void testExceptionAfter() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), new AbstractMessageProcessorTestCase.ExceptionThrowingMessageProcessor());
        process(builder.build(), getTestEventUsingFlow("0"));
    }

    @Test(expected = MessagingException.class)
    public void testExceptionBefore() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new AbstractMessageProcessorTestCase.ExceptionThrowingMessageProcessor(), getAppendingMP("1"));
        process(builder.build(), getTestEventUsingFlow("0"));
    }

    @Test(expected = MessagingException.class)
    public void testExceptionBetween() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(getAppendingMP("1"), new AbstractMessageProcessorTestCase.ExceptionThrowingMessageProcessor(),
                      getAppendingMP("2"));
        process(builder.build(), getTestEventUsingFlow("0"));
    }

    @Test(expected = MessagingException.class)
    public void testExceptionInResponse() throws MuleException, Exception
    {
        DefaultMessageProcessorChainBuilder builder = new DefaultMessageProcessorChainBuilder();
        builder.chain(new ResponseMessageProcessorAdapter(new AbstractMessageProcessorTestCase.ExceptionThrowingMessageProcessor()), getAppendingMP("1"));
        process(builder.build(), getTestEventUsingFlow("0"));
    }

    private MuleEvent process(MessageProcessor messageProcessor, MuleEvent event) throws Exception
    {
        MuleEvent result;
        if (nonBlocking && exchangePattern.hasResponse())
        {
            SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
            event = new DefaultMuleEvent(event, nullReplyToHandler);
            result = messageProcessor.process(event);
            if (NonBlockingVoidMuleEvent.getInstance() == result)
            {
                nullReplyToHandler.latch.await(1000, TimeUnit.MILLISECONDS);
                if (nullReplyToHandler.exception != null)
                {
                    throw nullReplyToHandler.exception;
                }
                else
                {
                    result = nullReplyToHandler.event;
                }
            }
        }
        else
        {
            result = messageProcessor.process(event);
        }
        return result;
    }

    private AppendingMP getAppendingMP(String append)
    {
        if (nonBlocking)
        {
            return new NonBlockingAppendingMP(append);
        }
        else
        {
            return new AppendingMP(append);
        }
    }

    private boolean isMultipleThreadsUsed()
    {
        return nonBlocking && exchangePattern.hasResponse() && !synchronous;
    }

    static class TestNonIntercepting implements MessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return new DefaultMuleEvent(new DefaultMuleMessage(event.getMessage().getPayload() + "MessageProcessor",
                                                               event.getMuleContext()), event);
        }
    }

    static class TestIntercepting extends AbstractInterceptingMessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return processNext(new DefaultMuleEvent(new DefaultMuleMessage(event.getMessage().getPayload() +
                                                                           "InterceptingMessageProcessor", event
                    .getMuleContext()), event));
        }
    }

    private void assertLifecycle(AppendingMP mp)
    {
        assertTrue(mp.flowConstuctInjected);
        assertTrue(mp.muleContextInjected);
        assertTrue(mp.initialised);
        assertTrue(mp.started);
        assertTrue(mp.stopped);
        assertTrue(mp.disposed);
    }

    private void assertLifecycle(AppendingInterceptingMP mp)
    {
        assertTrue(mp.flowConstuctInjected);
        assertTrue(mp.muleContextInjected);
        assertTrue(mp.initialised);
        assertTrue(mp.started);
        assertTrue(mp.stopped);
        assertTrue(mp.disposed);
    }

    class NonBlockingAppendingMP extends AppendingMP implements NonBlockingMessageProcessor
    {

        public NonBlockingAppendingMP(String append)
        {
            super(append);
        }
    }

    class AppendingMP implements MessageProcessor, Lifecycle, FlowConstructAware, MuleContextAware
    {

        String appendString;
        boolean muleContextInjected;
        boolean flowConstuctInjected;
        boolean initialised;
        boolean started;
        boolean stopped;
        boolean disposed;
        MuleEvent event;
        MuleEvent resultEvent;

        public AppendingMP(String append)
        {
            this.appendString = append;
        }

        @Override
        public MuleEvent process(final MuleEvent event) throws MuleException
        {
            if (nonBlocking && event.isAllowNonBlocking() && event.getReplyToHandler() != null)
            {
                executor.execute(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            threads++;
                            event.getReplyToHandler().processReplyTo(innerProcess(event), null, null);
                        }
                        catch (MessagingException e)
                        {
                            event.getReplyToHandler().processExceptionReplyTo(e, null);
                        }
                        catch (MuleException e)
                        {
                            e.printStackTrace();
                        }
                    }
                });
                return NonBlockingVoidMuleEvent.getInstance();

            }
            else
            {
                return innerProcess(event);
            }
        }

        private MuleEvent innerProcess(MuleEvent event)
        {
            this.event = event;
            MuleEvent result = new DefaultMuleEvent(new DefaultMuleMessage(event.getMessage().getPayload()
                                                                           + appendString, muleContext),
                                                    event);
            this.resultEvent = result;
            return result;
        }

        @Override
        public void initialise() throws InitialisationException
        {
            initialised = true;
        }

        @Override
        public void start() throws MuleException
        {
            started = true;
        }

        @Override
        public void stop() throws MuleException
        {
            stopped = true;
        }

        @Override
        public void dispose()
        {
            disposed = true;
        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }

        @Override
        public void setMuleContext(MuleContext context)
        {
            this.muleContextInjected = true;
        }

        @Override
        public void setFlowConstruct(FlowConstruct flowConstruct)
        {
            this.flowConstuctInjected = true;
        }
    }

    class AppendingInterceptingMP extends AbstractInterceptingMessageProcessor implements FlowConstructAware, Lifecycle
    {

        String appendString;
        boolean muleContextInjected;
        boolean flowConstuctInjected;
        boolean initialised;
        boolean started;
        boolean stopped;
        boolean disposed;
        MuleEvent event;
        MuleEvent resultEvent;
        private boolean stopProcessing;
        boolean invoked = false;

        public AppendingInterceptingMP(String appendString)
        {
            this(appendString, false);
        }

        public AppendingInterceptingMP(String appendString, boolean stopProcessing)
        {
            this.appendString = appendString;
            this.stopProcessing = stopProcessing;
        }

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            if (stopProcessing)
            {
                return event;
            }

            MuleEvent intermediateEvent = new DefaultMuleEvent(new DefaultMuleMessage(event.getMessage()
                                                                                              .getPayload() +
                                                                                      "before" + appendString,
                                                                                      DefaultMessageProcessorChainTestCase.this.muleContext), event);
            MuleEvent result = processNext(intermediateEvent);
            if (result != null && !result.equals(VoidMuleEvent.getInstance()) && !result.equals
                    (NonBlockingVoidMuleEvent.getInstance()))
            {
                return new DefaultMuleEvent(new DefaultMuleMessage(result.getMessage()
                                                                           .getPayload() + "after" + appendString,
                                                                   DefaultMessageProcessorChainTestCase.this
                                                                           .muleContext), result);
            }
            else
            {
                return result;
            }

        }

        @Override
        public void initialise() throws InitialisationException
        {
            initialised = true;
        }

        @Override
        public void start() throws MuleException
        {
            started = true;
        }

        @Override
        public void stop() throws MuleException
        {
            stopped = true;
        }

        @Override
        public void dispose()
        {
            disposed = true;
        }

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }

        @Override
        public void setMuleContext(MuleContext context)
        {
            this.muleContextInjected = true;
        }

        @Override
        public void setFlowConstruct(FlowConstruct flowConstruct)
        {
            this.flowConstuctInjected = true;
        }
    }

    static class ReturnNullMP implements MessageProcessor
    {

        MuleEvent event;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            this.event = event;
            return null;
        }
    }

    static class ReturnNullInterceptongMP extends AbstractInterceptingMessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return null;
        }
    }

    private static class ReturnVoidMP implements MessageProcessor
    {

        MuleEvent event;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            this.event = event;
            return VoidMuleEvent.getInstance();
        }
    }

    static class ReturnVoidMPInterceptongMP extends AbstractInterceptingMessageProcessor
    {

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return VoidMuleEvent.getInstance();
        }
    }

    protected MuleEvent getTestEventUsingFlow(Object data) throws Exception
    {
        MuleEvent event = mock(MuleEvent.class);
        MuleMessage message = new DefaultMuleMessage(data, muleContext);
        when(event.getId()).thenReturn(RandomStringUtils.randomNumeric(3));
        when(event.getMessage()).thenReturn(message);
        when(event.getExchangePattern()).thenReturn(exchangePattern);
        when(event.getMuleContext()).thenReturn(muleContext);
        Pipeline mockFlow = mock(Flow.class);
        when(mockFlow.getProcessingStrategy()).thenReturn(nonBlocking ? new NonBlockingProcessingStrategy() : new
                DefaultFlowProcessingStrategy());
        when(event.getFlowConstruct()).thenReturn(mockFlow);
        when(event.getSession()).thenReturn(mock(MuleSession.class));
        when(event.isSynchronous()).thenReturn(synchronous);
        when(event.isAllowNonBlocking()).thenReturn(!synchronous && nonBlocking);
        //when(event.isNonBlocking()).thenReturn(nonBlocking);
        return event;
    }

}
