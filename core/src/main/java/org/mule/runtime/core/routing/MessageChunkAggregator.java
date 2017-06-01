/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.serialization.SerializationException;
import org.mule.runtime.core.routing.correlation.CollectionCorrelatorCallback;
import org.mule.runtime.core.routing.correlation.CorrelationSequenceComparator;
import org.mule.runtime.core.routing.correlation.EventCorrelatorCallback;

import java.util.Arrays;
import java.util.Comparator;

import org.apache.commons.io.output.ByteArrayOutputStream;

public class MessageChunkAggregator extends AbstractAggregator {

  public static final int DEFAULT_BUFFER_SIZE = 4096;

  protected Comparator eventComparator;

  public MessageChunkAggregator() {
    super();
    eventComparator = new CorrelationSequenceComparator();
  }

  @Override
  protected EventCorrelatorCallback getCorrelatorCallback(MuleContext muleContext) {
    return new CollectionCorrelatorCallback(muleContext, storePrefix) {

      /**
       * This method is invoked if the shouldAggregate method is called and returns true. Once this method returns an aggregated
       * message the event group is removed from the router
       * 
       * @param events the event group for this request
       * @return an aggregated message
       * @throws org.mule.runtime.core.routing.AggregationException if the aggregation fails. in this scenario the whole event
       *         group is removed and passed to the exception handler for this componenet
       */
      @Override
      public Event aggregateEvents(EventGroup events) throws AggregationException {
        Event[] collectedEvents;
        try {
          collectedEvents = events.toArray(false);
        } catch (ObjectStoreException e) {
          throw new AggregationException(events, MessageChunkAggregator.this, e);
        }
        Event firstEvent = collectedEvents[0];
        Arrays.sort(collectedEvents, eventComparator);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);

        try {
          for (Event event : collectedEvents) {
            baos.write(event.getMessageAsBytes(muleContext));
          }

          final Message.Builder builder = Message.builder(firstEvent.getMessage());

          // try to deserialize message, since ChunkingRouter might have serialized the object...
          try {
            builder.payload(muleContext.getObjectSerializer().getInternalProtocol().deserialize(baos.toByteArray()));
          } catch (SerializationException e) {
            builder.payload(baos.toByteArray());
          }

          // Use last event, that hasn't been completed yet, for continued processing.
          return Event.builder(collectedEvents[collectedEvents.length - 1]).message(builder.build())
              .session(getMergedSession(events.toArray())).build();
        } catch (Exception e) {
          throw new AggregationException(events, MessageChunkAggregator.this, e);
        } finally {
          closeQuietly(baos);
        }
      }
    };
  }

}
