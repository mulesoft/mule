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
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

public class MessageEnricherTestCase extends AbstractMuleTestCase
{

    public void testEnrichHeaderWithPayload() throws MuleException, Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setEnricherExpression("header:myHeader");
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload("test");
                return event;
            }
        });

        assertEquals("test", enricher.process(getTestEvent("")).getMessage().getProperty("myHeader"));
    }

    public void testEnrichHeaderWithHeader() throws MuleException, Exception
    {
        MessageEnricher enricher = new MessageEnricher();
        enricher.setEnricherExpression("header:myHeader");
        enricher.setEvaluatorExpression("header:header1");
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setProperty("header1", "test");
                return event;
            }
        });

        assertEquals("test", enricher.process(getTestEvent("")).getMessage().getProperty("myHeader"));
    }

    public void testEnrichHeaderNestedEvaluator() throws MuleException, Exception
    {
        muleContext.getRegistry().registerObject("appender", new StringAppendTransformer(" append"));

        MessageEnricher enricher = new MessageEnricher();
        enricher.setEnricherExpression("header:myHeader");
        enricher.setEvaluatorExpression("#[process:appender:#[header:header1]]");
        enricher.setEnrichmentMessageProcessor(new MessageProcessor()
        {

            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setProperty("header1", "test");
                return event;
            }
        });

        MuleEvent event = getTestEvent("");
        RequestContext.setEvent(event);

        assertEquals("test append", enricher.process(event).getMessage().getProperty("myHeader"));
    }

}
