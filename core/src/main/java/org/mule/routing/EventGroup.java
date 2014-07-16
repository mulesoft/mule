/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.routing;

import org.mule.DefaultMessageCollection;
import org.mule.DefaultMuleEvent;
import org.mule.VoidMuleEvent;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleMessageCollection;
import org.mule.api.MuleSession;
import org.mule.api.config.MuleProperties;
import org.mule.api.store.ListableObjectStore;
import org.mule.api.store.ObjectStoreException;
import org.mule.api.store.ObjectStoreManager;
import org.mule.session.DefaultMuleSession;
import org.mule.util.ClassUtils;
import org.mule.util.store.DeserializationPostInitialisable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections.IteratorUtils;

/**
 * <code>EventGroup</code> is a holder over events grouped by a common group Id. This
 * can be used by components such as routers to managed related events.
 */
// @ThreadSafe
public class EventGroup implements Comparable<EventGroup>, Serializable, DeserializationPostInitialisable
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 953739659615692697L;

    public static final MuleEvent[] EMPTY_EVENTS_ARRAY = new MuleEvent[0];

    public static final String MULE_ARRIVAL_ORDER_PROPERTY = MuleProperties.PROPERTY_PREFIX + "ARRIVAL_ORDER";

    transient private ObjectStoreManager objectStoreManager = null;

    private final Object groupId;
    transient ListableObjectStore<MuleEvent> events;
    private final long created;
    private final int expectedSize;
    transient private MuleContext muleContext;
    private final String storePrefix;
    private String commonRootId = null;
    private static boolean hasNoCommonRootId = false;
    private int arrivalOrderCounter = 0;
    private Serializable lastStoredEventKey;

    public static final String DEFAULT_STORE_PREFIX = "DEFAULT_STORE";

    public EventGroup(Object groupId, MuleContext muleContext)
    {
        this(groupId, muleContext, -1, false, DEFAULT_STORE_PREFIX);
    }

    public EventGroup(Object groupId,
                      MuleContext muleContext,
                      int expectedSize,
                      boolean storeIsPersistent,
                      String storePrefix)
    {
        super();
        this.created = System.currentTimeMillis();
        this.muleContext = muleContext;
        this.storePrefix = storePrefix;

        String storeKey = storePrefix + ".eventGroup." + groupId;
        this.events = getObjectStoreManager().getObjectStore(storeKey, storeIsPersistent);

        this.expectedSize = expectedSize;
        this.groupId = groupId;
    }

    /**
     * Compare this EventGroup to another one. If the receiver and the argument both
     * have groupIds that are {@link Comparable}, they are used for the comparison;
     * otherwise - since the id can be any object - the group creation time stamp is
     * used as fallback. Older groups are considered "smaller".
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    public int compareTo(EventGroup other)
    {
        Object otherId = other.getGroupId();

        if (groupId instanceof Comparable<?> && otherId instanceof Comparable<?>)
        {
            return ((Comparable<Object>) groupId).compareTo(otherId);
        }
        else
        {
            long diff = created - other.getCreated();
            return (diff > 0 ? 1 : (diff < 0 ? -1 : 0));
        }
    }

    /**
     * Compares two EventGroups for equality. EventGroups are considered equal if
     * their groupIds (as returned by {@link #getGroupId()}) are equal.
     *
     * @see java.lang.Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof EventGroup))
        {
            return false;
        }

        final EventGroup other = (EventGroup) obj;
        if (groupId == null)
        {
            return (other.groupId == null);
        }

        return groupId.equals(other.groupId);
    }

    /**
     * The hashCode of an EventGroup is derived from the object returned by
     * {@link #getGroupId()}.
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return groupId.hashCode();
    }

    /**
     * Returns an identifier for this EventGroup. It is recommended that this id is
     * unique and {@link Comparable} e.g. a UUID.
     *
     * @return the id of this event group
     */
    public Object getGroupId()
    {
        return groupId;
    }

    /**
     * Returns an iterator over a snapshot copy of this group's collected events
     * sorted by their arrival time. If
     * you need to iterate over the group and e.g. remove select events, do so via
     * {@link #removeEvent(MuleEvent)}. If you need to do so atomically in order to
     * prevent e.g. concurrent reception/aggregation of the group during iteration,
     * wrap the iteration in a synchronized block on the group instance.
     *
     * @return an iterator over collected {@link MuleEvent}s.
     * @throws ObjectStoreException
     */
    public Iterator<MuleEvent> iterator() throws ObjectStoreException
    {
        return iterator(true);
    }

    /**
     * Returns an iterator over a snapshot copy of this group's collected events.,
     * optionally sorted by arrival order.  If
     * you need to iterate over the group and e.g. remove select events, do so via
     * {@link #removeEvent(MuleEvent)}. If you need to do so atomically in order to
     * prevent e.g. concurrent reception/aggregation of the group during iteration,
     * wrap the iteration in a synchronized block on the group instance.
     *
     * @return an iterator over collected {@link MuleEvent}s.
     * @throws ObjectStoreException
     */
    @SuppressWarnings("unchecked")
    public Iterator<MuleEvent> iterator(boolean sortByArrival) throws ObjectStoreException
    {
        synchronized (events)
        {
            if (events.allKeys().isEmpty())
            {
                return IteratorUtils.emptyIterator();
            }
            else
            {
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
    public MuleEvent[] toArray() throws ObjectStoreException
    {
        return toArray(true);
    }

    /**
     * Returns a snapshot of collected events in this group, optionally sorted by their arrival time.
     *
     * @return an array of collected {@link MuleEvent}s.
     * @throws ObjectStoreException
     */
    public MuleEvent[] toArray(boolean sortByArrival) throws ObjectStoreException
    {
        synchronized (events)
        {
            if (events.allKeys().isEmpty())
            {
                return EMPTY_EVENTS_ARRAY;
            }
            List<Serializable> keys = events.allKeys();
            MuleEvent[] eventArray = new MuleEvent[keys.size()];
            for (int i = 0; i < keys.size(); i++)
            {
                eventArray[i] = events.retrieve(keys.get(i));
            }
            if (sortByArrival)
            {
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
    public void addEvent(MuleEvent event) throws ObjectStoreException
    {
        synchronized (events)
        {
            //Using both event ID and CorrelationSequence since in certain instances
            //when an event is split up, the same event IDs are used.
            Serializable key=event.getId()+event.getMessage().getCorrelationSequence();
            event.getMessage().setInvocationProperty(MULE_ARRIVAL_ORDER_PROPERTY, ++arrivalOrderCounter);
            lastStoredEventKey = key;
            events.store(key, event);

            if (!hasNoCommonRootId)
            {
                String rootId = event.getMessage().getMessageRootId();
                if (commonRootId == null)
                {
                    commonRootId = rootId;
                }
                else if (!commonRootId.equals(rootId))
                {
                    hasNoCommonRootId = true;
                    commonRootId = null;
                }
            }
        }
    }


    /**
     * Remove the given event from the group.
     *
     * @param event the evnt to remove
     * @throws ObjectStoreException
     */
    public void removeEvent(MuleEvent event) throws ObjectStoreException
    {
        synchronized (events)
        {
            events.remove(event.getId());
        }
    }

    /**
     * Return the creation timestamp of the current group in milliseconds.
     *
     * @return the timestamp when this group was instantiated.
     */
    public long getCreated()
    {
        return created;
    }

    /**
     * Returns the number of events collected so far.
     *
     * @return number of events in this group or 0 if the group is empty.
     */
    public int size()
    {
        synchronized (events)
        {
            try
            {
                return events.allKeys().size();
            }
            catch (ObjectStoreException e)
            {
                // TODO Check if this is ok.
                return -1;
            }
        }
    }

    /**
     * Returns the number of events that this EventGroup is expecting before
     * correlation can proceed.
     *
     * @return expected number of events or -1 if no expected size was specified.
     */
    public int expectedSize()
    {
        return expectedSize;
    }

    /**
     * Removes all events from this group.
     *
     * @throws ObjectStoreException
     */
    public void clear() throws ObjectStoreException
    {
        getObjectStoreManager().disposeStore(events);
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder(80);
        buf.append(ClassUtils.getSimpleName(this.getClass()));
        buf.append(" {");
        buf.append("id=").append(groupId);
        buf.append(", expected size=").append(expectedSize);

        try
        {
            synchronized (events)
            {
                int currentSize;

                currentSize = events.allKeys().size();

                buf.append(", current events=").append(currentSize);

                if (currentSize > 0)
                {
                    buf.append(" [");
                    Iterator<Serializable> i = events.allKeys().iterator();
                    while (i.hasNext())
                    {
                        Serializable id = i.next();
                        buf.append(events.retrieve(id).getMessage().getUniqueId());
                        if (i.hasNext())
                        {
                            buf.append(", ");
                        }
                    }
                    buf.append(']');
                }
            }
        }
        catch (ObjectStoreException e)
        {
            buf.append("ObjectStoreException " + e + " caught:" + e.getMessage());
        }

        buf.append('}');

        return buf.toString();
    }

    public MuleMessageCollection toMessageCollection() throws ObjectStoreException
    {
        return toMessageCollection(true);
    }

    public MuleMessageCollection toMessageCollection(boolean sortByArrival) throws ObjectStoreException
    {
        DefaultMessageCollection col = new DefaultMessageCollection(muleContext);
        List<MuleMessage> messages = new ArrayList<MuleMessage>();

        synchronized (events)
        {
            for (Serializable id : events.allKeys())
            {
                MuleMessage message = events.retrieve(id).getMessage();
                messages.add(message);
            }
        }

        if (sortByArrival)
        {
            Collections.sort(messages, new ArrivalOrderMessageComparator());
        }
        col.addMessages(messages);
        return col;
    }

    public String getCommonRootId()
    {
        return commonRootId;
    }

    public MuleEvent getMessageCollectionEvent()
    {
        try
        {
            if (size() > 0)
            {

                MuleEvent lastEvent = retrieveLastStoredEvent();
                DefaultMuleEvent muleEvent = new DefaultMuleEvent(toMessageCollection(),
                                                                  lastEvent, getMergedSession());
                if (getCommonRootId() != null)
                {
                    muleEvent.getMessage().setMessageRootId(commonRootId);
                }
                return muleEvent;
            }
            else
            {
                return VoidMuleEvent.getInstance();
            }
        }
        catch (ObjectStoreException e)
        {
            // Nothing to do...
            return VoidMuleEvent.getInstance();
        }
    }

    private MuleEvent retrieveLastStoredEvent() throws ObjectStoreException
    {
        return events.retrieve(lastStoredEventKey);
    }

    protected MuleSession getMergedSession() throws ObjectStoreException
    {
        MuleEvent lastStoredEvent = retrieveLastStoredEvent();
        MuleSession session = new DefaultMuleSession(
                lastStoredEvent.getSession());
        for (Serializable key : events.allKeys())
        {
            if (!key.equals(lastStoredEventKey))
            {
                MuleEvent event = events.retrieve(key);
                addAndOverrideSessionProperties(session, event);
            }
        }
        addAndOverrideSessionProperties(session, lastStoredEvent);
        return session;
    }

    private void addAndOverrideSessionProperties(MuleSession session, MuleEvent event)
    {
        for (String name : event.getSession().getPropertyNamesAsSet())
        {
            session.setProperty(name, event.getSession().getProperty(name));
        }
    }

    private ObjectStoreManager getObjectStoreManager()
    {
        if (objectStoreManager == null)
        {
            objectStoreManager = muleContext.getRegistry().get(
                    MuleProperties.OBJECT_STORE_MANAGER);
        }
        return objectStoreManager;
    }

    public void initAfterDeserialisation(MuleContext context) throws MuleException
    {
        this.muleContext = context;

        String storeKey = storePrefix + ".eventGroup." + groupId;
        this.events = getObjectStoreManager().getObjectStore(storeKey, true);
    }

    public boolean isInitialised()
    {
        return muleContext != null;
    }

    public final class ArrivalOrderMessageComparator implements Comparator<MuleMessage>
    {
        @Override
        public int compare(MuleMessage message1, MuleMessage message2)
        {
            int val1 = message1.getInvocationProperty(MULE_ARRIVAL_ORDER_PROPERTY, -1);
            int val2 = message2.getInvocationProperty(MULE_ARRIVAL_ORDER_PROPERTY, -1);

            return val1 - val2;
        }
    }

    public final class ArrivalOrderEventComparator implements Comparator<MuleEvent>
    {
        @Override
        public int compare(MuleEvent event1, MuleEvent event2)
        {
            int val1 = event1.getMessage().getInvocationProperty(MULE_ARRIVAL_ORDER_PROPERTY, -1);
            int val2 = event2.getMessage().getInvocationProperty(MULE_ARRIVAL_ORDER_PROPERTY, -1);

            return val1 - val2;
        }
    }
}
