/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.el.mvel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.expression.InvalidExpressionException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.MuleManifest;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.Calendar;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

;

public class MVELExpressionLanguageTestCase extends AbstractMuleContextTestCase
{

    protected MVELExpressionLanguage mvel;

    @Before
    public void setupMVEL() throws InitialisationException
    {
        mvel = new MVELExpressionLanguage(muleContext);
        mvel.initialise();
    }

    @Test
    public void testEvaluateString()
    {
        // Literals
        assertEquals("hi", mvel.evaluate("'hi'"));
        assertEquals(4, mvel.evaluate("2*2"));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(), mvel.evaluate("server.timeZone"));
        assertEquals(MuleManifest.getProductVersion(), mvel.evaluate("mule.version"));
        assertEquals(muleContext.getConfiguration().getId(), mvel.evaluate("app.name"));
    }

    @Test
    public void testEvaluateStringMapOfStringObject()
    {
        // Literals
        assertEquals("hi", mvel.evaluate("'hi'", Collections.<String, Object> emptyMap()));
        assertEquals(4, mvel.evaluate("2*2", Collections.<String, Object> emptyMap()));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(),
            mvel.evaluate("server.timeZone", Collections.<String, Object> emptyMap()));
        assertEquals(MuleManifest.getProductVersion(),
            mvel.evaluate("mule.version", Collections.<String, Object> emptyMap()));
        assertEquals(muleContext.getConfiguration().getId(),
            mvel.evaluate("app.name", Collections.<String, Object> emptyMap()));

        // Custom variables (via method param)
        assertEquals(1, mvel.evaluate("foo", Collections.<String, Object> singletonMap("foo", 1)));
        assertEquals("bar", mvel.evaluate("foo", Collections.<String, Object> singletonMap("foo", "bar")));
    }

    @Test
    public void testEvaluateStringMuleEvent()
    {
        MuleEvent event = createMockEvent();

//        // Literals
//        assertEquals("hi", mvel.evaluate("'hi'", event));
//        assertEquals(4, mvel.evaluate("2*2", event));
//
//        // Static context
//        assertEquals(Calendar.getInstance().getTimeZone(), mvel.evaluate("server.timeZone", event));
//        assertEquals(MuleManifest.getProductVersion(), mvel.evaluate("mule.version", event));
//        assertEquals(muleContext.getConfiguration().getId(), mvel.evaluate("app.name", event));

        // Event context
        assertEquals("myFlow", mvel.evaluate("flow.name", event));
        assertEquals("foo", mvel.evaluate("message.payload", event));

    }

    @Test
    public void testEvaluateStringMuleEventMapOfStringObject()
    {
        MuleEvent event = createMockEvent();

        // Literals
        assertEquals("hi", mvel.evaluate("'hi'", event));
        assertEquals(4, mvel.evaluate("2*2", event));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(), mvel.evaluate("server.timeZone", event));
        assertEquals(MuleManifest.getProductVersion(), mvel.evaluate("mule.version", event));
        assertEquals(muleContext.getConfiguration().getId(), mvel.evaluate("app.name", event));

        // Event context
        assertEquals("myFlow", mvel.evaluate("flow.name", event));
        assertEquals("foo", mvel.evaluate("message.payload", event));

        // Custom variables (via method param)
        assertEquals(1, mvel.evaluate("foo", Collections.<String, Object> singletonMap("foo", 1)));
        assertEquals("bar", mvel.evaluate("foo", Collections.<String, Object> singletonMap("foo", "bar")));
    }

    @Test
    public void testEvaluateStringMuleMessage()
    {
        MuleMessage message = createMockMessage();

        // Literals
        assertEquals("hi", mvel.evaluate("'hi'", message));
        assertEquals(4, mvel.evaluate("2*2", message));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(), mvel.evaluate("server.timeZone", message));
        assertEquals(MuleManifest.getProductVersion(), mvel.evaluate("mule.version", message));
        assertEquals(muleContext.getConfiguration().getId(), mvel.evaluate("app.name", message));

        // Event context
        assertEquals("foo", mvel.evaluate("message.payload", message));
    }

    @Test
    public void testEvaluateStringMuleMessageMapOfStringObject()
    {
        MuleMessage message = createMockMessage();

        // Literals
        assertEquals("hi", mvel.evaluate("'hi'", message));
        assertEquals(4, mvel.evaluate("2*2", message));

        // Static context
        assertEquals(Calendar.getInstance().getTimeZone(), mvel.evaluate("server.timeZone", message));
        assertEquals(MuleManifest.getProductVersion(), mvel.evaluate("mule.version", message));
        assertEquals(muleContext.getConfiguration().getId(), mvel.evaluate("app.name", message));

        // Event context
        assertEquals("foo", mvel.evaluate("message.payload", message));

        // Custom variables (via method param)
        assertEquals(1, mvel.evaluate("foo", Collections.<String, Object> singletonMap("foo", 1)));
        assertEquals("bar", mvel.evaluate("foo", Collections.<String, Object> singletonMap("foo", "bar")));
    }

    @Test
    public void testIsValid()
    {
        assertTrue(mvel.isValid("2*2"));
    }

    @Test
    public void testIsValidInvalid()
    {
        assertFalse(mvel.isValid("2*'2"));
    }

    @Test
    public void testValidate()
    {
        mvel.validate("2*2");
    }

    @Test(expected = InvalidExpressionException.class)
    public void testValidateInvalid()
    {
        mvel.validate("2*'2");
    }

    protected MuleEvent createMockEvent()
    {
        MuleEvent event = mock(MuleEvent.class);
        FlowConstruct flowConstruct = mock(FlowConstruct.class);
        when(flowConstruct.getName()).thenReturn("myFlow");
        MuleMessage message = createMockMessage();
        Mockito.when(event.getFlowConstruct()).thenReturn(flowConstruct);
        Mockito.when(event.getMessage()).thenReturn(message);
        return event;
    }

    protected MuleMessage createMockMessage()
    {
        MuleMessage message = mock(MuleMessage.class);
        Mockito.when(message.getPayload()).thenReturn("foo");
        return message;
    }

}
