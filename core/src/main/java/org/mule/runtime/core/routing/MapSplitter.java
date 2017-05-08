/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.config.i18n.CoreMessages.objectNotOfCorrectType;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.Event;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Splits a message that has a map payload invoking the next message processor one for each item in the map in order. The Map
 * entry value is used as the new payload and the map key is set as a message property with the following property name 'key'.
 * <p>
 * <b>EIP Reference:</b> <a href="http://www.eaipatterns.com/Sequencer.html">http://www .eaipatterns.com/Sequencer.html</a>
 */
public class MapSplitter extends AbstractSplitter {

  public static String MAP_ENTRY_KEY = "key";

  @Override
  protected List<?> splitMessage(Event event) {
    Message message = event.getMessage();
    if (message.getPayload().getValue() instanceof Map<?, ?>) {
      List<Object> list = new LinkedList<>();
      Set<Map.Entry<?, ?>> set = ((Map) message.getPayload().getValue()).entrySet();
      for (Entry<?, ?> entry : set) {
        // TODO MULE-9502 Support "key" flowVar with MapSplitter in Mule 4
        list.add(entry.getValue());
      }
      return list;
    } else {
      throw new IllegalArgumentException(objectNotOfCorrectType(message.getPayload().getDataType().getType(), Map.class)
          .getMessage());
    }
  }
}
