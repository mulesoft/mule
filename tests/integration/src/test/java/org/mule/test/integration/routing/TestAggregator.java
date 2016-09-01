/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.routing.AbstractAggregator;
import org.mule.runtime.core.routing.AggregationException;
import org.mule.runtime.core.routing.EventGroup;
import org.mule.runtime.core.routing.correlation.CollectionCorrelatorCallback;
import org.mule.runtime.core.routing.correlation.EventCorrelatorCallback;

import java.util.Iterator;

public class TestAggregator extends AbstractAggregator {

  @Override
  protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext) {
    return new CollectionCorrelatorCallback(muleContext, storePrefix) {

      @Override
      public MuleEvent aggregateEvents(EventGroup events) throws AggregationException {
        StringBuilder buffer = new StringBuilder(128);

        try {
          for (Iterator<MuleEvent> iterator = events.iterator(); iterator.hasNext();) {
            MuleEvent event = iterator.next();
            try {
              buffer.append(event.transformMessageToString(muleContext));
            } catch (TransformerException e) {
              throw new AggregationException(events, null, e);
            }
          }
        } catch (ObjectStoreException e) {
          throw new AggregationException(events, null, e);
        }

        logger.debug("event payload is: " + buffer.toString());
        return MuleEvent.builder(events.getMessageCollectionEvent())
            .message(MuleMessage.builder().payload(buffer.toString()).build()).build();
      }
    };
  }
}
