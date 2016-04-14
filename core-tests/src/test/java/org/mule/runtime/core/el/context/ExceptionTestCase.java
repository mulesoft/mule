/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.DefaultMuleEvent;
import org.mule.MessageExchangePattern;
import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.DefaultExceptionPayload;

import org.junit.Test;

public class ExceptionTestCase extends AbstractELTestCase
{

    public ExceptionTestCase(Variant variant, String mvelOptimizer)
    {
        super(variant, mvelOptimizer);
    }

    @Test
    public void exception() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        RuntimeException rte = new RuntimeException();
        message.setExceptionPayload(new DefaultExceptionPayload(rte));
        assertEquals(rte, evaluate("exception", event));
    }

    @Test
    public void assignException() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        message.setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));
        assertImmutableVariable("exception='other'", event);
    }

    @Test
    public void exceptionCausedBy() throws Exception
    {
        MuleEvent event = getTestEvent("");
        MuleMessage message = event.getMessage();
        MessagingException me = new MessagingException(CoreMessages.createStaticMessage(""),
            new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestFlow()),
            new IllegalAccessException());
        message.setExceptionPayload(new DefaultExceptionPayload(me));
        assertTrue((Boolean) evaluate("exception.causedBy(java.lang.IllegalAccessException)", event));
    }
}
