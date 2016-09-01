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
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.transformer.simple.CombineCollectionsTransformer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class CombineCollectionsTransformerTestCase extends AbstractMuleContextTestCase {

  private CombineCollectionsTransformer merger;

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    merger = new CombineCollectionsTransformer();
  }

  @Test
  public void testMuleMessageCollectionMerge() throws Exception {
    MuleEvent event = getTestEvent("hello");

    List list = new ArrayList<>();
    list.add(MuleMessage.builder().collectionPayload(new String[] {"1", "2", "3"}).build());
    list.add(MuleMessage.builder().payload("4").build());
    list.add(MuleMessage.builder().collectionPayload(new String[] {"5", "6", "7"}).build());
    MuleMessage collection = MuleMessage.builder().collectionPayload(list, MuleMessage.class).build();

    event = MuleEvent.builder(event).message(collection).build();

    MuleEvent response = merger.process(event);

    assertTrue(response.getMessage().getPayload() instanceof List);
    assertEquals(7, ((List) response.getMessage().getPayload()).size());
  }

  @Test
  public void testMuleMessageMerge() throws Exception {
    MuleEvent event = getTestEvent("hello");

    List<Object> payload = new ArrayList<>();
    payload.add(Arrays.asList("1", "2", "3"));
    payload.add("4");
    payload.add(Arrays.asList("5", "6", "7"));
    event.setMessage(MuleMessage.builder(event.getMessage()).payload(payload).build());

    MuleEvent response = merger.process(event);

    assertTrue(response.getMessage().getPayload() instanceof List);
    assertEquals(7, ((List) response.getMessage().getPayload()).size());
  }
}
