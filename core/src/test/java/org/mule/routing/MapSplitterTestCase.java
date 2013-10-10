/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.routing;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MapSplitterTestCase extends AbstractMuleContextTestCase
{
    private MapSplitter mapSplitter;
    private List<String> splitPayloads = new ArrayList<String>();
    private List<String> splitKeyProperties = new ArrayList<String>();

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        mapSplitter = new MapSplitter();
        mapSplitter.setMuleContext(muleContext);
        mapSplitter.setListener(new MessageProcessor()
        {
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                splitPayloads.add(event.getMessageAsString());
                splitKeyProperties.add((String) event.getMessage().getProperty(MapSplitter.MAP_ENTRY_KEY,
                    PropertyScope.INVOCATION));
                return event;
            }
        });
    }

    @Test
    public void testSplit() throws Exception
    {
        Map<String, Object> testMap = new HashMap<String, Object>();
        testMap.put("1", "one");
        testMap.put("2", "two");
        testMap.put("3", "three");

        mapSplitter.process(getTestEvent(testMap));

        assertEquals(3, splitPayloads.size());
        assertTrue(splitPayloads.contains("one"));
        assertTrue(splitPayloads.contains("two"));
        assertTrue(splitPayloads.contains("three"));

        assertEquals(3, splitPayloads.size());
        assertEquals("1", splitKeyProperties.get(splitPayloads.indexOf("one")));
        assertEquals("2", splitKeyProperties.get(splitPayloads.indexOf("two")));
        assertEquals("3", splitKeyProperties.get(splitPayloads.indexOf("three")));
    }
}
