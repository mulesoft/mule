/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.mule.enricher;

import org.mule.RequestContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.enricher.MessageEnricher;
import org.mule.enricher.MessageEnricher.EnrichExpressionPair;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

public class MessageEnricherTestCase extends AbstractMuleTestCase
{

    public void testEnrichHeaderWithPayload() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload("test");
                return event;
            }
        });

        assertEquals("test", enricher.process(getTestEvent("")).getMessage().getOutboundProperty("myHeader"));
    }

    public void testEnrichHeaderWithHeader() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:header1]", "#[header:myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setOutboundProperty("header1", "test");
                return event;
            }
        });

        assertEquals("test", enricher.process(getTestEvent("")).getMessage().getOutboundProperty("myHeader"));
    }

    public void testEnrichHeadersMToN() throws Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:header1]", "#[header:myHeader1]"));
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:header2]", "#[header:myHeader2]"));
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[header:header3]", "#[header:myHeader3]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setOutboundProperty("header1", "test");
                event.getMessage().setOutboundProperty("header2", "test2");
                event.getMessage().setOutboundProperty("header3", "test3");
                return event;
            }
        });

        assertNull(enricher.process(getTestEvent("")).getMessage().getOutboundProperty("myHeader"));
        assertEquals("test2", enricher.process(getTestEvent(""))
            .getMessage()
            .getOutboundProperty("myHeader2"));
        assertEquals("test3", enricher.process(getTestEvent(""))
            .getMessage()
            .getOutboundProperty("myHeader3"));
    }

    public void testEnrichHeaderNestedEvaluator() throws Exception
    {
        muleContext.getRegistry().registerObject("appender", new StringAppendTransformer(" append"));

        MessageEnricher enricher = new MessageEnricher();
        enricher.addEnrichExpressionPair(new EnrichExpressionPair("#[process:appender:#[header:header1]]",
            "#[header:myHeader]"));
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setOutboundProperty("header1", "test");
                return event;
            }
        });

        MuleEvent event = getTestEvent("");
        RequestContext.setEvent(event);

        assertEquals("test append", enricher.process(getTestEvent("")).getMessage().getOutboundProperty(
            "myHeader"));
    }

}
