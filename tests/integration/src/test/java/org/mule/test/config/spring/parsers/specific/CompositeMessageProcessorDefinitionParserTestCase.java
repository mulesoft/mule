/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers.specific;

import static org.junit.Assert.assertEquals;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.processor.MessageProcessor;

import org.junit.Test;

public class CompositeMessageProcessorDefinitionParserTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/parsers/specific/composite-message-processor.xml";
    }

    @Test
    public void testInterceptingComposite() throws Exception
    {
        MessageProcessor composite = muleContext.getRegistry().lookupObject("composite1");
        assertEquals("0123", composite.process(getTestEvent("0")).getMessageAsString());
    }

    @Test
    public void testInterceptingNestedComposite() throws Exception
    {
        MessageProcessor composite = muleContext.getRegistry().lookupObject("composite2");
        assertEquals("01abc2", composite.process(getTestEvent("0")).getMessageAsString());
    }

}
