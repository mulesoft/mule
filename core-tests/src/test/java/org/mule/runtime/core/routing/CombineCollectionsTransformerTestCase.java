/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.DefaultMuleMessage;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.runtime.core.transformer.simple.CombineCollectionsTransformer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

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
        
        List list = new ArrayList<>();
        list.add(new DefaultMuleMessage(Arrays.asList("1", "2", "3"), muleContext));
        list.add(new DefaultMuleMessage("4", muleContext));
        list.add(new DefaultMuleMessage(Arrays.asList("5", "6", "7"), muleContext));

        MuleMessage collection = new DefaultMuleMessage(list, muleContext);
        event = new DefaultMuleEvent(collection, event);
        
        MuleEvent response = merger.process(event);
        
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
        event.setMessage(event.getMessage().transform(msg -> {
            msg.setPayload(payload);
            return msg;
        }));
        
        MuleEvent response = merger.process(event);
        
        assertTrue(response.getMessage().getPayload() instanceof List);
        assertEquals(7, ((List)response.getMessage().getPayload()).size());
    }
}
