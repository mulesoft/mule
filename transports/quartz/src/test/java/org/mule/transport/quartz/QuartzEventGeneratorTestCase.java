/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.quartz;

import org.mule.api.MuleEventContext;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;

import java.util.ArrayList;
import java.util.List;

public class QuartzEventGeneratorTestCase extends FunctionalTestCase
{
    
    private static final String PAYLOAD = "TRIGGER STRING";

    private final List<String> receivedPayloads = new ArrayList<String>();
    
    @Override
    protected String getConfigResources()
    {
        return "quartz-event-generator.xml";
    }

    public void testEventGeneratorPayload() throws Exception
    {
        FunctionalTestComponent component = (FunctionalTestComponent) getComponent("quartzService");
        assertNotNull(component);
        CountdownCallback callback = new Callback(1, receivedPayloads);
        component.setEventCallback(callback);

        // wait for incoming messages
        assertTrue(callback.await(60000));
        assertTrue(receivedPayloads.size() > 0);
        assertEquals(PAYLOAD, receivedPayloads.get(0));
    }
    
    private static class Callback extends CountdownCallback
    {
        private List<String> messageList;

        public Callback(int messagesExpected, List<String> payloadStore)
        {
            super(messagesExpected);
            messageList = payloadStore;
        }

        @Override
        public void eventReceived(MuleEventContext context, Object component) throws Exception
        {
            synchronized (this)
            {
                String payloadString = context.getMessage().getPayloadAsString();
                messageList.add(payloadString);
            }
            
            super.eventReceived(context, component);
        }        
    }
    
}


