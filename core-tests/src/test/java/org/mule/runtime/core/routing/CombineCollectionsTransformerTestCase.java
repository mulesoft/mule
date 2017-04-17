/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.internal.message.InternalMessage;
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
    List list = new ArrayList<>();
    list.add(Message.builder().collectionPayload(new String[] {"1", "2", "3"}).build());
    list.add(Message.of("4"));
    list.add(Message.builder().collectionPayload(new String[] {"5", "6", "7"}).build());
    Message collection = Message.builder().collectionPayload(list, InternalMessage.class).build();

    Event event = Event.builder(testEvent()).message(collection).build();

    Event response = merger.process(event);

    assertTrue(response.getMessage().getPayload().getValue() instanceof List);
    assertEquals(7, ((List) response.getMessage().getPayload().getValue()).size());
  }

  @Test
  public void testMuleMessageMerge() throws Exception {
    List<Object> payload = new ArrayList<>();
    payload.add(Arrays.asList("1", "2", "3"));
    payload.add("4");
    payload.add(Arrays.asList("5", "6", "7"));
    Event event =
        Event.builder(testEvent()).message(InternalMessage.builder(testEvent().getMessage()).payload(payload).build()).build();

    Event response = merger.process(event);

    assertTrue(response.getMessage().getPayload().getValue() instanceof List);
    assertEquals(7, ((List) response.getMessage().getPayload().getValue()).size());
  }
}
