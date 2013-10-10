/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessageCollection;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.transformer.simple.CombineCollectionsTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CombineCollectionsTransformerTestCase extends AbstractMuleContextTestCase
{
    private CombineCollectionsTransformer merger;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        merger = new CombineCollectionsTransformer();
    }

    @Test
    public void testMuleMessageCollectionMerge() throws Exception
    {   
        MuleEvent event = getTestEvent("hello");
        
        DefaultMessageCollection collection = new DefaultMessageCollection(muleContext);
        collection.addMessage(new DefaultMuleMessage(Arrays.asList("1", "2", "3"), muleContext));
        collection.addMessage(new DefaultMuleMessage("4", muleContext));
        collection.addMessage(new DefaultMuleMessage(Arrays.asList("5", "6", "7"), muleContext));
        
        event = new DefaultMuleEvent(collection, event);
        
        MuleEvent response = merger.process(event);
        
        assertFalse(response.getMessage() instanceof MuleMessageCollection);
        
        assertTrue(response.getMessage().getPayload() instanceof List);
        assertEquals(7, ((List)response.getMessage().getPayload()).size());
    }
    
    @Test
    public void testMuleMessageMerge() throws Exception
    {
        MuleEvent event = getTestEvent("hello");
        
        ArrayList<Object> payload = new ArrayList<Object>();
        payload.add(Arrays.asList("1", "2", "3"));
        payload.add("4");
        payload.add(Arrays.asList("5", "6", "7"));
        event.getMessage().setPayload(payload);
        
        MuleEvent response = merger.process(event);
        
        assertFalse(response.getMessage() instanceof MuleMessageCollection);
        
        assertTrue(response.getMessage().getPayload() instanceof List);
        assertEquals(7, ((List)response.getMessage().getPayload()).size());
    }
}
