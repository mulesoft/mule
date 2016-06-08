/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.mule.enricher;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.transformer.types.MimeTypes.JSON;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.MessageExchangePattern;
import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.ThreadSafeAccess;
import org.mule.runtime.core.api.connector.ReplyToHandler;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.config.DefaultMuleConfiguration;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.enricher.MessageEnricher;
import org.mule.runtime.core.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChain;
import org.mule.runtime.core.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.runtime.core.transformer.types.DataTypeFactory;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import junit.framework.Assert;

public class MessageEnricherTestCase extends AbstractMuleContextTestCase
{

    public static final String FOO_FLOW_VAR_EXPRESSION = "#[flowVars['foo']]";

    @Test
    public void testEnrichHeaderWithPayload() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload("test");
                return event;
            }
        });
        enricher.initialise();

        MuleMessage result = enricher.process(getTestEvent("")).getMessage();
        assertEquals("test", result.getOutboundProperty("myHeader"));
        assertEquals("", result.getPayload());
    }

    @Test
    public void testEnrichHeaderWithHeader() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.header1]", "#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setOutboundProperty("header1", "test");
                return event;
            }
        });

        MuleMessage result = enricher.process(getTestEvent("")).getMessage();
        assertEquals("test", result.getOutboundProperty("myHeader"));
        assertEquals("", result.getPayload());
    }

    @Test
    public void testEnrichHeadersMToN() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.header1]", "#[message.outboundProperties.myHeader1]"));
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.header2]", "#[message.outboundProperties.myHeader2]"));
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.header3]", "#[message.outboundProperties.myHeader3]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setOutboundProperty("header1", "test");
                event.getMessage().setOutboundProperty("header2", "test2");
                event.getMessage().setOutboundProperty("header3", "test3");
                return event;
            }
        });

        MuleMessage result = enricher.process(getTestEvent("")).getMessage();

        assertNull(result.getOutboundProperty("myHeader"));
        assertEquals("test2", result.getOutboundProperty("myHeader2"));
        assertEquals("test3", result.getOutboundProperty("myHeader3"));

        assertEquals("", result.getPayload());
    }

    @Test
    public void testEnrichWithNullResponse() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return null;
            }
        });

        MuleMessage result = enricher.process(getTestEvent("")).getMessage();
        assertNull(result.getOutboundProperty("myHeader"));
        assertEquals("", result.getPayload());
    }

    @Test
    public void testEnrichWithException() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                throw new MessagingException(CoreMessages.createStaticMessage("Expected"), event);
            }
        });

        try
        {
            enricher.process(getTestEvent(""));
            fail("Expected a MessagingException");
        }
        catch (MessagingException e)
        {
            assertThat(e.getMessage(), is("Expected."));
        }
        assertThat(RequestContext.getEvent().getReplyToHandler(), nullValue());
    }

    @Test
    public void propogateMessage() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload("enriched");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");
        in.getMessage().setOutboundProperty("foo", "bar");
        MuleEvent out = enricher.process(in);
        Assert.assertSame(in, out);
        Assert.assertSame(in.getMessage(), out.getMessage());
        assertEquals(in.getMessage().getUniqueId(), out.getMessage().getUniqueId());
        assertEquals(in.getMessage().getOutboundPropertyNames(), out.getMessage().getOutboundPropertyNames());
        assertEquals("bar", out.getMessage().getOutboundProperty("foo"));
        assertEquals(in.getMessage().getPayload(), out.getMessage().getPayload());
    }

    @Test
    public void propogateMessagePropagateSession() throws Exception
    {
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setEnricherPropagatesSessionVariableChanges(true);
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload("enriched");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");
        in.getMessage().setOutboundProperty("foo", "bar");
        MuleEvent out = enricher.process(in);
        Assert.assertNotSame(in, out);
        Assert.assertSame(in.getMessage(), out.getMessage());
        assertEquals(in.getMessage().getUniqueId(), out.getMessage().getUniqueId());
        assertEquals(in.getMessage().getOutboundPropertyNames(), out.getMessage().getOutboundPropertyNames());
        assertEquals("bar", out.getMessage().getOutboundProperty("foo"));
        assertEquals("", out.getMessage().getPayload());
    }

    @Test
    public void propagatesVariables() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload("enriched");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");
        in.getSession().setProperty("sessionFoo", "bar");
        in.setFlowVariable("flowFoo", "bar");

        MuleEvent out = enricher.process(in);

        assertEquals("bar", out.getSession().getProperty("sessionFoo"));
        assertEquals("bar", out.getFlowVariable("flowFoo"));
    }

    @Test
    public void doNotImplicitlyEnrichMessagePayload() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload("enriched");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");

        MuleEvent out = enricher.process(in);

        assertEquals("", out.getMessage().getPayload());
    }

    @Test
    public void doNotImplicitlyEnrichMessageProperties() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setOutboundProperty("foo", "bar");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");

        MuleEvent out = enricher.process(in);

        assertNull(out.getMessage().getOutboundProperty("foo"));
    }

    @Test
    public void doNotImplicitlyEnrichFlowVariable() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.setFlowVariable("flowFoo", "bar");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");

        MuleEvent out = enricher.process(in);

        assertNull(out.getFlowVariable("flowFoo"));
    }

    @Test
    public void doNotImplicitlyEnrichSessionVariable() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getSession().setProperty("sessionFoo", "bar");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");

        MuleEvent out = enricher.process(in);

        assertNull(out.getSession().getProperty("sessionFoo"));
    }

    @Test
    public void propagatesSession() throws Exception
    {
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setEnricherPropagatesSessionVariableChanges(true);
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[message.outboundProperties.myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getSession().setProperty("sessionFoo", "bar");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");

        MuleEvent out = enricher.process(in);

        assertEquals("bar", out.getSession().getProperty("sessionFoo"));
    }

    @Test
    public void enrichFlowVariable() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[flowVars.foo]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload("bar");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");

        MuleEvent out = enricher.process(in);

        assertEquals("bar", out.getFlowVariable("foo"));
    }

    @Test
    public void enrichSessionVariable() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[sessionVars['foo']]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload("bar");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");

        MuleEvent out = enricher.process(in);

        assertEquals("bar", out.getSession().getProperty("foo"));
    }

    @Test
    public void enrichesFlowVarWithDataType() throws Exception
    {
        doEnrichDataTypePropagationTest(new EnrichExpressionPair("#[payload]", FOO_FLOW_VAR_EXPRESSION));
    }

    @Test
    public void enrichesFlowVarWithDataTypeUsingExpressionEvaluator() throws Exception
    {
        doEnrichDataTypePropagationTest(new EnrichExpressionPair(FOO_FLOW_VAR_EXPRESSION));
    }

    @Test
    public void enricherConservesSameEventInstance() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[sessionVars['foo']]"));
        SensingNullMessageProcessor sensingNullMessageProcessor = new SensingNullMessageProcessor();
        enricher.setEnrichmentMessageProcessor(sensingNullMessageProcessor);

        MuleEvent in = new DefaultMuleEvent(new DefaultMuleMessage(TEST_MESSAGE, muleContext),
                                            MessageExchangePattern.REQUEST_RESPONSE, mock(Flow.class));
        MuleEvent out = enricher.process(in);

        assertThat(out, is(sameInstance(in)));
        assertThat(sensingNullMessageProcessor.event, not(sameInstance(in)));
    }

    @Test
    public void enricherConservesSameEventInstanceNonBlockingTargetNonBlocking() throws Exception
    {
        SensingNullMessageProcessor sensingNullMessageProcessor = new SensingNullMessageProcessor();
        MessageEnricher enricher = createNonBlockingEnricher(sensingNullMessageProcessor);
        SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
        final MuleEvent in = createNonBlockingEvent(nullReplyToHandler);

        MuleEvent out = processEnricherInChain(enricher, in);

        nullReplyToHandler.latch.await(RECEIVE_TIMEOUT, TimeUnit.MILLISECONDS);

        assertThat(sensingNullMessageProcessor.event.getMessage(), not(sameInstance(in.getMessage())));

        assertThat(out, is(instanceOf(NonBlockingVoidMuleEvent.class)));
        assertThat(nullReplyToHandler.event.getMessage(), is(sameInstance(in.getMessage())));
    }

    @Test
    public void enricherConservesSameEventInstanceNonBlockingTargetBlocking() throws Exception
    {
        SensingNullMessageProcessor sensingNullMessageProcessor = new SensingNullMessageProcessor(){
            @Override
            public boolean isNonBlocking(MuleEvent event)
            {
                return false;
            }
        };
        MessageEnricher enricher = createNonBlockingEnricher(sensingNullMessageProcessor);

        SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
        final MuleEvent in = createNonBlockingEvent(nullReplyToHandler);

        MuleEvent out = processEnricherInChain(enricher, in);

        assertThat(sensingNullMessageProcessor.event.getMessage(), not(sameInstance(in.getMessage())));
        assertThat(out.getMessage(), is(sameInstance(in.getMessage())));
    }

    @Test
    public void testEnrichWithExceptionNonBlocking() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                throw new MessagingException(CoreMessages.createStaticMessage("Expected"), event);
            }
        });

        try
        {
            SensingNullReplyToHandler nullReplyToHandler = new SensingNullReplyToHandler();
            enricher.process(createNonBlockingEvent(nullReplyToHandler));
            fail("Expected a MessagingException");
        }
        catch (MessagingException e)
        {
            assertThat(e.getMessage(), is("Expected."));
        }
        assertThat(RequestContext.getEvent().getReplyToHandler(), instanceOf(ReplyToHandler.class));
    }

    private MuleEvent createNonBlockingEvent(SensingNullReplyToHandler nullReplyToHandler)
    {
        Flow flow = mock(Flow.class);
        when(flow.getProcessingStrategy()).thenReturn(new NonBlockingProcessingStrategy());

        return new DefaultMuleEvent(new DefaultMuleMessage(TEST_MESSAGE, muleContext),
                                                  MessageExchangePattern.REQUEST_RESPONSE, nullReplyToHandler,
                                                  flow);
    }

    private MessageEnricher createNonBlockingEnricher(SensingNullMessageProcessor sensingNullMessageProcessor)
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[sessionVars['foo']]"));
        enricher.setEnrichmentMessageProcessor(sensingNullMessageProcessor);
        return enricher;
    }

    private MuleEvent processEnricherInChain(MessageEnricher enricher, final MuleEvent in) throws MuleException
    {
        return DefaultMessageProcessorChain.from(enricher, new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                // Ensure that message is writable after being processed by enricher with non-blocking target
                ((ThreadSafeAccess) event).assertAccess(true);
                assertThat(event.getMessage(), is(sameInstance(in.getMessage())));
                return event;
            }
        }).process(in);
    }

    private void doEnrichDataTypePropagationTest(EnrichExpressionPair pair) throws Exception
    {
        final DataType<?> dataType = DataTypeFactory.create(String.class, JSON);
        dataType.setEncoding(StandardCharsets.UTF_16.name());

        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);

        enricher.addEnrichExpressionPair(pair);
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {

                event.getMessage().setPayload("bar", dataType);
                return event;
            }
        });
        MuleEvent in = getTestEvent("");

        MuleEvent out = enricher.process(in);

        assertEquals("bar", out.getFlowVariable("foo"));
        assertThat(out.getFlowVariableDataType("foo"), DataTypeMatcher.like(String.class, JSON, StandardCharsets.UTF_16.name()));
    }
}
