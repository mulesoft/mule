/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class MapSplitterTestCase extends AbstractMuleContextTestCase {

  private MapSplitter mapSplitter;
  private List<String> splitPayloads = new ArrayList<String>();
  private List<String> splitKeyProperties = new ArrayList<String>();

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    mapSplitter = new MapSplitter();
    mapSplitter.setMuleContext(muleContext);
    mapSplitter.setListener(new MessageProcessor() {

      public MuleEvent process(MuleEvent event) throws MuleException {
        splitPayloads.add(event.getMessageAsString());
        return event;
      }
    });
  }

  @Test
  public void testSplit() throws Exception {
    Map<String, Object> testMap = new HashMap<String, Object>();
    testMap.put("1", "one");
    testMap.put("2", "two");
    testMap.put("3", "three");

    mapSplitter.process(getTestEvent(testMap));

    assertEquals(3, splitPayloads.size());
    assertTrue(splitPayloads.contains("one"));
    assertTrue(splitPayloads.contains("two"));
    assertTrue(splitPayloads.contains("three"));
  }
}
