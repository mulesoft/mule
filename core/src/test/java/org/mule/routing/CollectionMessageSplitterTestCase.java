/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.routing;

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.service.Service;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectionMessageSplitterTestCase extends AbstractMuleTestCase
{
    public CollectionMessageSplitterTestCase()
    {
        setStartContext(true);
    }

    public void testRouter() throws Exception
    {
        Service fc = getTestService();
        MuleSession session = getTestSession(fc, muleContext);

        Map<String, Object> inboundProps = new HashMap();
        inboundProps.put("inbound1", "1");
        inboundProps.put("inbound2", 2);
        inboundProps.put("inbound3", session);

        Map<String, Object> outboundProps = new HashMap();
        inboundProps.put("outbound1", "3");
        inboundProps.put("outbound2", 4);
        inboundProps.put("outbound3", session);

        Map<String, Object> invocationProps = new HashMap();
        inboundProps.put("invoke1", "5");
        inboundProps.put("invoke2", 6);
        inboundProps.put("invoke3", session);

        List<String> payload = Arrays.asList("abc", "def", "ghi");
        MuleMessage toSplit = new DefaultMuleMessage(payload, inboundProps, outboundProps, null, muleContext);
        for (Map.Entry<String, Object> entry : invocationProps.entrySet())
        {
            toSplit.setInvocationProperty(entry.getKey(), entry.getValue());
        }
        CollectionSplitter splitter = new CollectionSplitter();
        splitter.setMuleContext(muleContext);
        Grabber grabber = new Grabber();
        splitter.setListener(grabber);
        DefaultMuleEvent event = new DefaultMuleEvent(toSplit, getTestInboundEndpoint("ep"), session);
        splitter.process(event);
        List<MuleMessage> splits =  grabber.getMessages();
        assertEquals(3, splits.size());
        for (MuleMessage msg : splits)
        {
            assertTrue(msg.getPayload() instanceof String);
            String str = (String) msg.getPayload();
            assertTrue(payload.contains(str));
            for (String key : inboundProps.keySet())
            {
                assertEquals(msg.getInboundProperty(key), inboundProps.get(key));
            }
            for (String key : outboundProps.keySet())
            {
                assertEquals(msg.getOutboundProperty(key), outboundProps.get(key));
            }
            for (String key : invocationProps.keySet())
            {
                assertEquals(msg.getInvocationProperty(key), invocationProps.get(key));
            }
        }
    }

    static class Grabber implements MessageProcessor
    {
        private List<MuleMessage> messages = new ArrayList();

        public MuleEvent process(MuleEvent event) throws MuleException
        {
            messages.add(event.getMessage());
            return null;
        }

        public List<MuleMessage> getMessages()
        {
            return messages;
        }
    }
}