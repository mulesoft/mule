/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.mule.enricher;

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
import static org.mule.transformer.types.MimeTypes.JSON;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.NonBlockingVoidMuleEvent;
import org.mule.RequestContext;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.api.transport.ReplyToHandler;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.i18n.CoreMessages;
import org.mule.construct.Flow;
import org.mule.enricher.MessageEnricher;
import org.mule.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.processor.chain.DefaultMessageProcessorChain;
import org.mule.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.transformer.simple.StringAppendTransformer;
import org.mule.transformer.types.DataTypeFactory;

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
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
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
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:header1]", "#[header:myHeader]"));
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
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:header1]", "#[header:myHeader1]"));
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:header2]", "#[header:myHeader2]"));
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:header3]", "#[header:myHeader3]"));
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
    public void testEnrichHeaderNestedEvaluator() throws Exception
    {
        muleContext.getRegistry().registerObject("appender", new StringAppendTransformer(" append"));

        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[process:appender:#[header:header1]]",
                                                                  "#[header:myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setOutboundProperty("header1", "test");
                return event;
            }
        });

        MuleEvent event = getTestEvent("");
        RequestContext.setEvent(event);

        assertEquals("test append",
                     enricher.process(getTestEvent("")).getMessage().getOutboundProperty("myHeader"));
    }

    @Test
    public void testEnrichWithNullResponse() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
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
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
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
        assertEquals(in.getMessage().getPropertyNames(), out.getMessage().getPropertyNames());
        assertEquals("bar", out.getMessage().getOutboundProperty("foo"));
        assertEquals(in.getMessage().getPayload(), out.getMessage().getPayload());
    }

    @Test
    public void propogateMessagePropagateSession() throws Exception
    {
        ((DefaultMuleConfiguration) muleContext.getConfiguration()).setEnricherPropagatesSessionVariableChanges(true);
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
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
        assertEquals(in.getMessage().getPropertyNames(), out.getMessage().getPropertyNames());
        assertEquals("bar", out.getMessage().getOutboundProperty("foo"));
        assertEquals("", out.getMessage().getPayload());
    }

    @Test
    public void propagatesVariables() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
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
        in.getMessage().setInvocationProperty("flowFoo", "bar");

        MuleEvent out = enricher.process(in);

        assertEquals("bar", out.getSession().getProperty("sessionFoo"));
        assertEquals("bar", out.getMessage().getInvocationProperty("flowFoo"));
    }

    @Test
    public void doNotImplicitlyEnrichMessagePayload() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
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
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
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
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setInvocationProperty("flowFoo", "bar");
                return event;
            }
        });
        MuleEvent in = getTestEvent("");

        MuleEvent out = enricher.process(in);

        assertNull(out.getMessage().getInvocationProperty("flowFoo"));
    }

    @Test
    public void doNotImplicitlyEnrichSessionVariable() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
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
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
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
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[variable:foo]"));
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

        assertEquals("bar", out.getMessage().getInvocationProperty("foo"));
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

        assertEquals("bar", out.getSessionVariable("foo"));
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

        assertEquals("bar", out.getMessage().getInvocationProperty("foo"));
        assertThat(out.getMessage().getPropertyDataType("foo", PropertyScope.INVOCATION), DataTypeMatcher.like(String.class, JSON, StandardCharsets.UTF_16.name()));
    }
}
