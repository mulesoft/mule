/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.message.DefaultExceptionPayload;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.transformer.simple.StringAppendTransformer;

import java.util.ArrayList;
import java.util.List;

public class FirstSuccessfulTestCase extends AbstractMuleTestCase
{
    private static final String EXCEPTION_SEEN = "EXCEPTION WAS SEEN";

    public FirstSuccessfulTestCase()
    {
        setStartContext(true);
    }

    public void testFirstSuccessful() throws Exception
    {
        MuleSession session = getTestSession(getTestService(), muleContext);
        FirstSuccessful fs = new FirstSuccessful();
        fs.setMuleContext(muleContext);
        List<MessageProcessor> routes = new ArrayList<MessageProcessor>();
        routes.add(new TestProcessor("abc"));
        routes.add(new TestProcessor("def"));
        routes.add(new TestProcessor("ghi"));
        fs.setMessageProcessors(routes);
        fs.setMuleContext(muleContext);
        fs.initialise();
        assertEquals("No abc", getPayload(fs, session, ""));
        assertEquals("No def", getPayload(fs, session, "abc"));
        assertEquals("No ghi", getPayload(fs, session, "abcdef"));
        assertEquals(EXCEPTION_SEEN, getPayload(fs, session, "abcdefghi"));
        assertEquals("No def", getPayload(fs, session, "ABC"));
        assertEquals("No ghi", getPayload(fs, session, "ABCDEF"));
        assertEquals(EXCEPTION_SEEN, getPayload(fs, session, "ABCDEFGHI"));
    }

    public void testFailureExpression() throws MuleException, Exception
    {
        FirstSuccessful fs = new FirstSuccessful();
        fs.setFailureExpression("#[payload-type:java.lang.Integer]");
        List<MessageProcessor> routes = new ArrayList<MessageProcessor>();
        routes.add(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                event.getMessage().setPayload(Integer.valueOf(1));
                return event;
            }
        });
        routes.add(new StringAppendTransformer("abc"));
        fs.setMessageProcessors(routes);
        fs.setMuleContext(muleContext);
        fs.initialise();
        assertEquals("abc", fs.process(getTestEvent("")).getMessageAsString());
    }

    public void testRouteReturnsNullEvent() throws MuleException, Exception
    {
        FirstSuccessful fs = new FirstSuccessful();
        List<MessageProcessor> routes = new ArrayList<MessageProcessor>();
        routes.add(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return null;
            }
        });
        fs.setMessageProcessors(routes);
        fs.setMuleContext(muleContext);
        fs.initialise();
        assertNull(fs.process(getTestEvent("")));
    }

    public void testRouteReturnsNullMessage() throws MuleException, Exception
    {
        FirstSuccessful fs = new FirstSuccessful();
        List<MessageProcessor> routes = new ArrayList<MessageProcessor>();
        routes.add(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                return new DefaultMuleEvent(null, event);
            }
        });
        fs.setMessageProcessors(routes);
        fs.setMuleContext(muleContext);
        fs.initialise();
        try
        {
            fs.process(getTestEvent(""));
            fail("Exception expected");
        }
        catch (CouldNotRouteOutboundMessageException e)
        {

        }
    }

    private String getPayload(MessageProcessor mp, MuleSession session, String message) throws Exception
    {
        MuleMessage msg = new DefaultMuleMessage(message, muleContext);
        try
        {
            MuleEvent event = mp.process(new DefaultMuleEvent(msg, null, session));
            MuleMessage returnedMessage = event.getMessage();
            if (returnedMessage.getExceptionPayload() != null)
            {
                return EXCEPTION_SEEN;
            }
            else
            {
                return returnedMessage.getPayloadAsString();
            }
        }
        catch (Exception ex)
        {
            return EXCEPTION_SEEN;
        }
    }

    static class TestProcessor implements MessageProcessor
    {
        private String rejectIfMatches;

        TestProcessor(String rejectIfMatches)
        {
            this.rejectIfMatches = rejectIfMatches;
        }

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            try
            {
                DefaultMuleMessage msg;
                String payload = event.getMessage().getPayloadAsString();
                if (payload.indexOf(rejectIfMatches) >= 0)
                {
                    throw new DefaultMuleException("Saw " + rejectIfMatches);
                }
                else if (payload.toLowerCase().indexOf(rejectIfMatches) >= 0)
                {
                    msg = new DefaultMuleMessage(null, muleContext);
                    msg.setExceptionPayload(new DefaultExceptionPayload(new Exception()));
                }
                else
                {
                    msg = new DefaultMuleMessage("No " + rejectIfMatches, muleContext);
                }
                return new DefaultMuleEvent(msg, null, event.getSession());
            }
            catch (Exception e)
            {
                throw new DefaultMuleException(e);
            }
        }
    }
}
