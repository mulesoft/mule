/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.message.Correlation.NOT_SET;

import org.mule.runtime.core.VoidMuleEvent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.CollectionBuilder;
import org.mule.runtime.core.api.MuleSession;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.core.api.store.ObjectStoreException;
import org.mule.runtime.core.api.store.PartitionableObjectStore;
import org.mule.runtime.core.session.DefaultMuleSession;
import org.mule.runtime.core.util.ClassUtils;
import org.mule.runtime.core.util.store.DeserializationPostInitialisable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.IteratorUtils;

/**
 * <code>EventGroup</code> is a holder over events grouped by a common group Id. This can be used by components such as routers to
 * managed related events.
 */
// @ThreadSafe
public class EventGroup implements Comparable<EventGroup>, Serializable, DeserializationPostInitialisable {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 953739659615692697L;

  public static final MuleEvent[] EMPTY_EVENTS_ARRAY = new MuleEvent[0];

  public static final String MULE_ARRIVAL_ORDER_PROPERTY = MuleProperties.PROPERTY_PREFIX + "ARRIVAL_ORDER";

  private final Object groupId;
  private transient PartitionableObjectStore<MuleEvent> eventsObjectStore;
  private final String storePrefix;
  private final String eventsPartitionKey;
  private final long created;
  private final Integer expectedSize;
  transient private MuleContext muleContext;
  private int arrivalOrderCounter = 0;
  private Serializable lastStoredEventKey;

  public static final String DEFAULT_STORE_PREFIX = "DEFAULT_STORE";

  public EventGroup(Object groupId, MuleContext muleContext) {
    this(groupId, muleContext, Optional.empty(), DEFAULT_STORE_PREFIX);
  }

  public EventGroup(Object groupId, MuleContext muleContext, Optional<Integer> expectedSize, String storePrefix) {
    super();
    this.created = System.currentTimeMillis();
    this.muleContext = muleContext;

    this.storePrefix = storePrefix;
    this.eventsPartitionKey = storePrefix + ".eventGroups." + groupId;

    this.expectedSize = expectedSize.orElse(null);
    this.groupId = groupId;
  }

  /**
   * Compare this EventGroup to another one. If the receiver and the argument both have groupIds that are {@link Comparable}, they
   * are used for the comparison; otherwise - since the id can be any object - the group creation time stamp is used as fallback.
   * Older groups are considered "smaller".
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  @SuppressWarnings("unchecked")
  public int compareTo(EventGroup other) {
    Object otherId = other.getGroupId();

    if (groupId instanceof Comparable<?> && otherId instanceof Comparable<?>) {
      return ((Comparable<Object>) groupId).compareTo(otherId);
    } else {
      long diff = created - other.getCreated();
      return (diff > 0 ? 1 : (diff < 0 ? -1 : 0));
    }
  }

  /**
   * Compares two EventGroups for equality. EventGroups are considered equal if their groupIds (as returned by
   * {@link #getGroupId()}) are equal.
   *
   * @see java.lang.Object#equals(Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof EventGroup)) {
      return false;
    }

    final EventGroup other = (EventGroup) obj;
    if (groupId == null) {
      return (other.groupId == null);
    }

    return groupId.equals(other.groupId);
  }

  /**
   * The hashCode of an EventGroup is derived from the object returned by {@link #getGroupId()}.
   *
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return groupId.hashCode();
  }

  /**
   * Returns an identifier for this EventGroup. It is recommended that this id is unique and {@link Comparable} e.g. a UUID.
   *
   * @return the id of this event group
   */
  public Object getGroupId() {
    return groupId;
  }

  /**
   * Returns an iterator over a snapshot copy of this group's collected events sorted by their arrival time. If you need to
   * iterate over the group and e.g. remove select events, do so via {@link #removeEvent(MuleEvent)}. If you need to do so
   * atomically in order to prevent e.g. concurrent reception/aggregation of the group during iteration, wrap the iteration in a
   * synchronized block on the group instance.
   *
   * @return an iterator over collected {@link MuleEvent}s.
   * @throws ObjectStoreException
   */
  public Iterator<MuleEvent> iterator() throws ObjectStoreException {
    return iterator(true);
  }

  /**
   * Returns an iterator over a snapshot copy of this group's collected events., optionally sorted by arrival order. If you need
   * to iterate over the group and e.g. remove select events, do so via {@link #removeEvent(MuleEvent)}. If you need to do so
   * atomically in order to prevent e.g. concurrent reception/aggregation of the group during iteration, wrap the iteration in a
   * synchronized block on the group instance.
   *
   * @return an iterator over collected {@link MuleEvent}s.
   * @throws ObjectStoreException
   */
  @SuppressWarnings("unchecked")
  public Iterator<MuleEvent> iterator(boolean sortByArrival) throws ObjectStoreException {
    synchronized (this) {
      if (eventsObjectStore.allKeys(eventsPartitionKey).isEmpty()) {
        return IteratorUtils.emptyIterator();
      } else {
        return IteratorUtils.arrayIterator(this.toArray(sortByArrival));
      }
    }
  }


  /**
   * Returns a snapshot of collected events in this group sorted by their arrival time.
   *
   * @return an array of collected {@link MuleEvent}s.
   * @throws ObjectStoreException
   */
  public MuleEvent[] toArray() throws ObjectStoreException {
    return toArray(true);
  }

  /**
   * Returns a snapshot of collected events in this group, optionally sorted by their arrival time.
   *
   * @return an array of collected {@link MuleEvent}s.
   * @throws ObjectStoreException
   */
  public MuleEvent[] toArray(boolean sortByArrival) throws ObjectStoreException {
    synchronized (this) {
      if (eventsObjectStore.allKeys(eventsPartitionKey).isEmpty()) {
        return EMPTY_EVENTS_ARRAY;
      }
      List<Serializable> keys = eventsObjectStore.allKeys(eventsPartitionKey);
      MuleEvent[] eventArray = new MuleEvent[keys.size()];
      for (int i = 0; i < keys.size(); i++) {
        eventArray[i] = eventsObjectStore.retrieve(keys.get(i), eventsPartitionKey);
      }
      if (sortByArrival) {
        Arrays.sort(eventArray, new ArrivalOrderEventComparator());
      }
      return eventArray;
    }
  }

  /**
   * Add the given event to this group.
   *
   * @param event the event to add
   * @throws ObjectStoreException
   */
  public void addEvent(MuleEvent event) throws ObjectStoreException {
    synchronized (this) {
      // Using both event ID and CorrelationSequence since in certain instances
      // when an event is split up, the same event IDs are used.
      Serializable key = getEventKey(event);
      event.setFlowVariable(MULE_ARRIVAL_ORDER_PROPERTY, ++arrivalOrderCounter);
      lastStoredEventKey = key;
      eventsObjectStore.store(key, event, eventsPartitionKey);
    }
  }

  private String getEventKey(MuleEvent event) {
    return event.getId() + event.getCorrelation().getSequence().map(v -> v.toString()).orElse(NOT_SET);
  }

  /**
   * Remove the given event from the group.
   *
   * @param event the evnt to remove
   * @throws ObjectStoreException
   */
  public void removeEvent(MuleEvent event) throws ObjectStoreException {
    synchronized (this) {
      eventsObjectStore.remove(event.getId(), eventsPartitionKey);
    }
  }

  /**
   * Return the creation timestamp of the current group in milliseconds.
   *
   * @return the timestamp when this group was instantiated.
   */
  public long getCreated() {
    return created;
  }

  /**
   * Returns the number of events collected so far.
   *
   * @return number of events in this group or 0 if the group is empty.
   */
  public int size() {
    synchronized (this) {
      try {
        return eventsObjectStore.allKeys(eventsPartitionKey).size();
      } catch (ObjectStoreException e) {
        // TODO Check if this is ok.
        return -1;
      }
    }
  }

  /**
   * Returns the number of events that this EventGroup is expecting before correlation can proceed.
   *
   * @return expected number of events or null if no expected size was specified.
   */
  public Optional<Integer> expectedSize() {
    return Optional.ofNullable(expectedSize);
  }

  /**
   * Removes all events from this group.
   *
   * @throws ObjectStoreException
   */
  public void clear() throws ObjectStoreException {
    synchronized (this) {
      eventsObjectStore.clear(eventsPartitionKey);
      eventsObjectStore.close(eventsPartitionKey);
    }
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(80);
    buf.append(ClassUtils.getSimpleName(this.getClass()));
    buf.append(" {");
    buf.append("id=").append(groupId);
    buf.append(", expected size=").append(expectedSize().map(v -> v.toString()).orElse(NOT_SET));

    try {
      synchronized (this) {
        int currentSize;

        currentSize = eventsObjectStore.allKeys(eventsPartitionKey).size();

        buf.append(", current events=").append(currentSize);

        if (currentSize > 0) {
          buf.append(" [");
          Iterator<Serializable> i = eventsObjectStore.allKeys(eventsPartitionKey).iterator();
          while (i.hasNext()) {
            Serializable id = i.next();
            buf.append(eventsObjectStore.retrieve(id, eventsPartitionKey).getContext().getCorrelationId());
            if (i.hasNext()) {
              buf.append(", ");
            }
          }
          buf.append(']');
        }
      }
    } catch (ObjectStoreException e) {
      buf.append("ObjectStoreException " + e + " caught:" + e.getMessage());
    }

    buf.append('}');

    return buf.toString();
  }

  public MuleEvent getMessageCollectionEvent() {
    try {
      if (size() > 0) {

        MuleEvent[] muleEvents = toArray(true);

        List<MuleMessage> messageList = Arrays.stream(muleEvents).map(event -> event.getMessage()).collect(toList());

        final CollectionBuilder builder = MuleMessage.builder().collectionPayload(messageList, MuleMessage.class);
        MuleEvent muleEvent =
            MuleEvent.builder(retrieveLastStoredEvent()).message(builder.build()).session(getMergedSession()).build();
        return muleEvent;
      } else {
        return VoidMuleEvent.getInstance();
      }
    } catch (ObjectStoreException e) {
      // Nothing to do...
      return VoidMuleEvent.getInstance();
    }
  }

  private MuleEvent retrieveLastStoredEvent() throws ObjectStoreException {
    synchronized (this) {
      if (lastStoredEventKey == null) {
        lastStoredEventKey = findLastStoredEventKey();
      }

      return eventsObjectStore.retrieve(lastStoredEventKey, eventsPartitionKey);
    }
  }

  protected MuleSession getMergedSession() throws ObjectStoreException {
    MuleEvent lastStoredEvent = retrieveLastStoredEvent();
    MuleSession session = new DefaultMuleSession(lastStoredEvent.getSession());
    for (Serializable key : eventsObjectStore.allKeys(eventsPartitionKey)) {
      if (!key.equals(lastStoredEventKey)) {
        MuleEvent event = eventsObjectStore.retrieve(key, eventsPartitionKey);
        addAndOverrideSessionProperties(session, event);
      }
    }
    addAndOverrideSessionProperties(session, lastStoredEvent);
    return session;
  }

  private void addAndOverrideSessionProperties(MuleSession session, MuleEvent event) {
    for (String name : event.getSession().getPropertyNamesAsSet()) {
      session.setProperty(name, event.getSession().getProperty(name));
    }
  }

  public void initAfterDeserialisation(MuleContext context) throws MuleException {
    this.muleContext = context;
  }

  public void initEventsStore(PartitionableObjectStore<MuleEvent> events) throws ObjectStoreException {
    this.eventsObjectStore = events;
    events.open(eventsPartitionKey);
  }

  /**
   * Finds the last stored event key on the event group.
   * <p/>
   * Event group uses {@code lastStoredEventKey} internally to differentiate the last event added to the group. When the event
   * group is stored in an {@link org.mule.runtime.core.api.store.ObjectStore} that serializes/deserializes the instances, the
   * eventGroup state is not keep updated. When an instance is deserialized, the events field is restored, but no the
   * lastStoredEventKey field. As {@link #lastStoredEventKey} is used only under some scenarios and the cost of finding the last
   * event key is high, is better to use lazy initialization for that field.
   *
   * @return the key of that last event added to the group. Null if no events added yet.
   * @throws ObjectStoreException
   */
  private String findLastStoredEventKey() throws ObjectStoreException {
    final MuleEvent[] muleEvents = toArray(true);

    if (muleEvents.length > 0) {
      return getEventKey(muleEvents[muleEvents.length - 1]);
    }

    return null;
  }

  public boolean isInitialised() {
    return muleContext != null;
  }

  public final class ArrivalOrderEventComparator implements Comparator<MuleEvent> {

    @Override
    public int compare(MuleEvent event1, MuleEvent event2) {
      return getEventOrder(event1) - getEventOrder(event2);
    }

    private int getEventOrder(MuleEvent event) {
      Integer orderVariable = event.getFlowVariable(MULE_ARRIVAL_ORDER_PROPERTY);
      return orderVariable != null ? orderVariable : -1;
    }
  }
}
