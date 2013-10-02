/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
