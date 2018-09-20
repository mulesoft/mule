/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.mule.enricher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.transformer.types.MimeTypes.JSON;

import java.nio.charset.StandardCharsets;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.ThreadSafeAccess;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.construct.Flow;
import org.mule.enricher.MessageEnricher;
import org.mule.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.processor.chain.DefaultMessageProcessorChain;
import org.mule.processor.strategy.NonBlockingProcessingStrategy;
import org.mule.tck.SensingNullMessageProcessor;
import org.mule.tck.SensingNullReplyToHandler;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.DataTypeMatcher;
import org.mule.transformer.types.DataTypeFactory;

public abstract class AbstractEnricherTestCase extends AbstractMuleContextTestCase
{

    public static final String FOO_FLOW_VAR_EXPRESSION = "#[flowVars['foo']]";


    protected MuleEvent createNonBlockingEvent(SensingNullReplyToHandler nullReplyToHandler)
    {
        Flow flow = mock(Flow.class);
        when(flow.getProcessorPath(any(MessageProcessor.class))).thenReturn("testPath" + "");
        when(flow.getProcessingStrategy()).thenReturn(new NonBlockingProcessingStrategy());

        return new DefaultMuleEvent(new DefaultMuleMessage(TEST_MESSAGE, muleContext),
                MessageExchangePattern.REQUEST_RESPONSE, nullReplyToHandler,
                flow);
    }

    protected MuleEvent createBlockingEvent()
    {
        return createNonBlockingEvent(null);
    }

    protected MessageEnricher createEnricher(SensingNullMessageProcessor sensingNullMessageProcessor)
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setMuleContext(muleContext);
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[sessionVars['foo']]"));
        enricher.setEnrichmentMessageProcessor(sensingNullMessageProcessor);
        return enricher;
    }

    protected MuleEvent processEnricherInChain(MessageEnricher enricher, final MuleEvent in) throws MuleException
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

    protected void doEnrichDataTypePropagationTest(EnrichExpressionPair pair) throws Exception
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
