/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.el.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.MessageExchangePattern;
import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.config.i18n.CoreMessages;
import org.mule.message.DefaultExceptionPayload;

import org.junit.Test;

public class ExceptionTestCase extends AbstractELTestCase
{

    public ExceptionTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void exception() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        RuntimeException rte = new RuntimeException();
        message.setExceptionPayload(new DefaultExceptionPayload(rte));
        assertEquals(rte, evaluate("exception", message));
    }

    @Test
    public void assignException() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        message.setExceptionPayload(new DefaultExceptionPayload(new RuntimeException()));
        assertImmutableVariable("exception='other'", message);
    }

    @Test
    public void exceptionCausedBy() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("", muleContext);
        MessagingException me = new MessagingException(CoreMessages.createStaticMessage(""),
            new DefaultMuleEvent(message, MessageExchangePattern.ONE_WAY, getTestService()),
            new IllegalAccessException());
        message.setExceptionPayload(new DefaultExceptionPayload(me));
        assertTrue((Boolean) evaluate("exception.causedBy(java.lang.IllegalAccessException)", message));
    }
}
