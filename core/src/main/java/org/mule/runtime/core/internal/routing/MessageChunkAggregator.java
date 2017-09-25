/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import static org.mule.runtime.core.api.util.IOUtils.closeQuietly;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.serialization.SerializationException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.routing.correlation.CollectionCorrelatorCallback;
import org.mule.runtime.core.internal.routing.correlation.CorrelationSequenceComparator;
import org.mule.runtime.core.internal.routing.correlation.EventCorrelatorCallback;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.util.Arrays;
import java.util.Comparator;

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
       * @throws AggregationException if the aggregation fails. in this scenario the whole event
       *         group is removed and passed to the exception handler for this componenet
       */
      @Override
      public CoreEvent aggregateEvents(EventGroup events) throws AggregationException {
        PrivilegedEvent[] collectedEvents;
        try {
          collectedEvents = events.toArray(false);
        } catch (ObjectStoreException e) {
          throw new AggregationException(events, MessageChunkAggregator.this, e);
        }
        CoreEvent firstEvent = collectedEvents[0];
        Arrays.sort(collectedEvents, eventComparator);
        ByteArrayOutputStream baos = new ByteArrayOutputStream(DEFAULT_BUFFER_SIZE);

        try {
          for (PrivilegedEvent event : collectedEvents) {
            baos.write(event.getMessageAsBytes(muleContext));
          }

          final Message.Builder builder = Message.builder(firstEvent.getMessage());

          // try to deserialize message, since ChunkingRouter might have serialized the object...
          try {
            builder.value(muleContext.getObjectSerializer().getInternalProtocol().deserialize(baos.toByteArray()));
          } catch (SerializationException e) {
            builder.value(baos.toByteArray());
          }

          // Use last event, that hasn't been completed yet, for continued processing.
          return PrivilegedEvent.builder(collectedEvents[collectedEvents.length - 1]).message(builder.build())
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
