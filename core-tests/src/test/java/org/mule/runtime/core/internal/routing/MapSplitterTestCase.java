/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.tck.util.MuleContextUtils.eventBuilder;

import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapSplitterTestCase extends AbstractMuleContextTestCase {

  private Splitter mapSplitter;
  private List<Map.Entry<String, String>> splitPayloads = new ArrayList<>();

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    mapSplitter = new Splitter();
    mapSplitter.setListener(event -> {
      splitPayloads.add((Map.Entry<String, String>) event.getMessage().getPayload().getValue());
      return event;
    });
    mapSplitter.setMuleContext(muleContext);
    mapSplitter.initialise();
  }

  @Test
  public void testSplit() throws Exception {
    Map<String, Object> testMap = new HashMap<>();
    testMap.put("1", "one");
    testMap.put("2", "two");
    testMap.put("3", "three");

    mapSplitter.process(eventBuilder(muleContext).message(of(testMap)).build());

    assertThat(splitPayloads, hasSize(3));
    assertThat(splitPayloads.get(0), instanceOf(Map.Entry.class));
    assertThat(splitPayloads.get(0).getKey(), is("1"));
    assertThat(splitPayloads.get(0).getValue(), is("one"));
    assertThat(splitPayloads.get(1).getKey(), is("2"));
    assertThat(splitPayloads.get(1).getValue(), is("two"));
    assertThat(splitPayloads.get(2).getKey(), is("3"));
    assertThat(splitPayloads.get(2).getValue(), is("three"));
  }
}
