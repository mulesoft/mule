/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.mule.runtime.api.metadata.DataType.MULE_MESSAGE_COLLECTION;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 
 * Takes a payload which is a Collection of Collections and turns into a single List. For example, if the payload is a Collection
 * which contains a Collection with elements A and B and another Collection with elements C and D, this will turn them into a
 * single Collection with elements A, B, C and D.
 * 
 * This transformer will also work on MuleMessageCollections. In this case, it will take the individual Collection payloads of
 * each Message and merge them into a single Collection on a new Message.
 */
public class CombineCollectionsTransformer extends AbstractAnnotatedObject implements Processor {

  @Override
  public Event process(Event event) throws MuleException {
    Message msg = event.getMessage();

    List<Object> payload = new ArrayList<>();
    Class<?> itemType = Object.class;
    if (MULE_MESSAGE_COLLECTION.isCompatibleWith(msg.getPayload().getDataType())) {
      itemType = Message.class;
      for (Message child : (Collection<Message>) msg.getPayload().getValue()) {
        Object childPayload = child.getPayload().getValue();
        if (childPayload instanceof Collection) {
          payload.addAll((Collection) childPayload);
        } else {
          payload.add(childPayload);
        }
      }
    } else if (msg.getPayload().getValue() instanceof Collection) {
      add(payload, (Collection) msg.getPayload().getValue());
    } else {
      itemType = msg.getPayload().getDataType().getType();
      payload.add(msg.getPayload().getValue());
    }

    Message listMessage = Message.builder(msg).collectionPayload(payload, itemType).build();
    return Event.builder(event).message(listMessage).build();
  }

  private void add(List<Object> newPayload, Collection existingPayload) {
    for (Object o : existingPayload) {
      if (o instanceof Collection) {
        newPayload.addAll((Collection) o);
      } else {
        newPayload.add(o);
      }
    }
  }
}
