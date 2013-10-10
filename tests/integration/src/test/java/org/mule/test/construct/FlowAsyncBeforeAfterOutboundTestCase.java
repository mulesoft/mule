/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

public class FlowAsyncBeforeAfterOutboundTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/construct/flow-async-before-after-outbound.xml";
    }

    @Test
    public void testAsyncBefore() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        MuleMessage msgSync = client.send("vm://test.before.sync.in", "message", null);
        
        MuleMessage msgAsync = client.request("vm://test.before.async.out", RECEIVE_TIMEOUT);
        MuleMessage msgOut = client.request("vm://test.before.out", RECEIVE_TIMEOUT);
               
        assertCorrectThreads(msgSync, msgAsync, msgOut);
        
    }
    
    @Test
    public void testAsyncAfter() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        
        MuleMessage msgSync = client.send("vm://test.after.sync.in", "message", null);
        
        MuleMessage msgAsync = client.request("vm://test.after.async.out", RECEIVE_TIMEOUT);
        MuleMessage msgOut = client.request("vm://test.after.out", RECEIVE_TIMEOUT);
        
        assertCorrectThreads(msgSync, msgAsync, msgOut);
    }
    
    private void assertCorrectThreads(MuleMessage msgSync, MuleMessage msgAsync, MuleMessage msgOut) throws Exception
    {
        assertNotNull(msgSync);
        assertNotNull(msgAsync);
        assertNotNull(msgOut);
        
        assertEquals(msgSync.getInboundProperty("request-response-thread"), 
            msgOut.getInboundProperty("request-response-thread"));
                
        assertTrue(!msgAsync.getInboundProperty("async-thread").
            equals(msgSync.getInboundProperty("request-response-thread")));
        
        assertTrue(!msgAsync.getInboundProperty("async-thread").
            equals(msgOut.getInboundProperty("request-response-thread")));   
    }
    
    public static class ThreadSensingMessageProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            String propName = event.getMessage().getInvocationProperty("property-name");
            
            event.getMessage().setOutboundProperty(propName, Thread.currentThread().getName());
            return event;
        }
    }

}
