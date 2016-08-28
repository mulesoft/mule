/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.transformer.simple;

import static org.mule.runtime.api.metadata.DataType.MULE_MESSAGE_COLLECTION;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.processor.MessageProcessor;

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
 * each MuleMessage and merge them into a single Collection on a new MuleMessage.
 */
public class CombineCollectionsTransformer implements MessageProcessor {

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    MuleMessage msg = event.getMessage();

    List<Object> payload = new ArrayList<>();
    Class<?> itemType = Object.class;
    if (MULE_MESSAGE_COLLECTION.isCompatibleWith(msg.getDataType())) {
      itemType = MuleMessage.class;
      for (MuleMessage child : (Collection<MuleMessage>) msg.getPayload()) {
        Object childPayload = child.getPayload();
        if (childPayload instanceof Collection) {
          payload.addAll((Collection) childPayload);
        } else {
          payload.add(childPayload);
        }
      }
    } else if (msg.getPayload() instanceof Collection) {
      add(payload, (Collection) msg.getPayload());
    } else {
      itemType = msg.getDataType().getType();
      payload.add(msg.getPayload());
    }

    MuleMessage listMessage = MuleMessage.builder(msg).collectionPayload(payload, itemType).build();
    return MuleEvent.builder(event).message(listMessage).build();
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
