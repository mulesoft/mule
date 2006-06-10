/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.routing.response;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
import edu.emory.mathcs.backport.java.util.concurrent.locks.Lock;
import edu.emory.mathcs.backport.java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.routing.inbound.EventGroup;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOMessage;
import org.mule.umo.routing.ResponseTimeoutException;
import org.mule.umo.routing.RoutingException;
import org.mule.util.PropertiesUtils;
import org.mule.util.concurrent.Latch;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>AbstractResponseAggregator</code> provides a base class for
 * implementing response aggregator routers. This provides a thread-safe
 * implemenetation and allows developers to customise how and when events are
 * grouped and collated.
 *
 * Response Agrregators are used to collect responses that are usually sent to replyTo endpoints set
 * on outbound routers. When an event is sent out via an outbound router, the response router will block the
 * response flow on an UMOComponent until the Response Router resolves a reply or times out.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractResponseAggregator extends AbstractResponseRouter
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected Map responseEvents = new ConcurrentHashMap();
    private Map locks = new HashMap();

    protected Map eventGroups = new ConcurrentHashMap();
    private Lock locksCollectionLock = new ReentrantLock();

    public void process(UMOEvent event) throws RoutingException
    {
        AtomicBoolean doAggregate = new AtomicBoolean(false);
        EventGroup eg = addEvent(event);
        doAggregate.compareAndSet(false, shouldAggregate(eg));

        if (doAggregate.get()) {
            UMOMessage returnMessage = aggregateEvents(eg);
            Object id = eg.getGroupId();
            removeGroup(id);

            responseEvents.put(id, returnMessage);
            Lock l = null;
            locksCollectionLock.lock();
            l = (Lock) locks.get(id);
            if (l == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Creating latch for " + id + " in " + this);
                }
                l = new Latch();
                if(locks.get(id)!=null) {
                    throw new IllegalStateException("There is already a lock with ID: " + id);
                }
                locks.put(id, l);
            }
            locksCollectionLock.unlock();
            l.unlock();
        }
    }

    /**
     * Adds the event to an event group. Groups are defined by the correlationId
     * on the message. If no 'correlation Id' is returned from calling <code>getReplyAggregateIdentifier()</code>
     * a routing exception will be thrown
     * 
     * @param event the reply event received by the response router
     * @return The event group for the current event or a new group if the current event
     * doesn't belong to an existing group
     */
    protected EventGroup addEvent(UMOEvent event) throws RoutingException
    {
        Object cId = getReplyAggregateIdentifier(event.getMessage());

        if (cId == null || cId.equals("-1")) {
            throw new RoutingException(new Message(Messages.NO_CORRELATION_ID), event.getMessage(), event.getEndpoint());
        }

        EventGroup eg = (EventGroup) eventGroups.get(cId);
        if (logger.isDebugEnabled()) {
            logger.debug("Adding event to response aggregator group: " + cId);
        }
        if (eg == null) {
            eg = createEventGroup(cId, event);
            eg.addEvent(event);
            eventGroups.put(eg.getGroupId(), eg);
        } else {
            eg.addEvent(event);
        }
        return eg;
    }

    /**
     * Creates a new event group with the given Id and can use other properties on the event
     * Custom implementations can even overload the eventGroup object here
     * @param id The Event group Id for the new Group
     * @param event the current event
     * @return a New event group for the incoming event
     */
    protected EventGroup createEventGroup(Object id, UMOEvent event) {
        return new EventGroup(id);
    }

    protected void removeGroup(Object id)
    {
        eventGroups.remove(id);
    }

    public UMOMessage getResponse(UMOMessage message) throws RoutingException
    {
        Object responseId = getCallResponseAggregateIdentifier(message);

        if (logger.isDebugEnabled()) {
            logger.debug("Waiting for response for message id: " + responseId + " in " + this);
        }

        Lock l = null;
        locksCollectionLock.lock();
            l = (Lock) locks.get(responseId);
            if (l == null) {
                logger.debug("Got response but no one is waiting for it yet. Creating latch for "
                        + responseId + " in " + this);
                l = new Latch();
                 if(locks.get(responseId)!=null) {
                    throw new IllegalStateException("There is already a lock with ID: " + responseId);
                }
                locks.put(responseId, l);
            } else {
                logger.debug("Got latch for message: " + responseId);
            }
        locksCollectionLock.unlock();

        boolean b = false;
        try {
            logger.debug("Waiting for response to message: " + responseId);
            if (getTimeout() <= 0) {
                l.lock();
                b = true;
            } else {
                b = l.tryLock(this.getTimeout(), TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
        if (!b) {
            if(logger.isTraceEnabled()) {
                synchronized(responseEvents) {
                    logger.trace("Current responses are: \n" +
                    PropertiesUtils.propertiesToString(responseEvents, true));
                }
            }
            throw new ResponseTimeoutException(new Message(Messages.RESPONSE_TIMED_OUT_X_WAITING_FOR_ID_X,
                                                           String.valueOf(getTimeout()),
                                                           responseId), message, null);
        }

        UMOMessage result = (UMOMessage) responseEvents.remove(responseId);
        locks.remove(responseId);
        if (result == null) {
            // this should never happen ,just using it as a safe gaurd for now
            throw new IllegalStateException("Response Message is null");
        }
        return result;
    }

    /**
     * Determines if the event group is ready to be aggregated. if the group is
     * ready to be aggregated (this is entirely up to the application. it could
     * be determined by volume, last modified time or some oher criteria based
     * on the last event received)
     * 
     * @param events
     * @return true if the event gorep is ready for aggregation
     */
    protected abstract boolean shouldAggregate(EventGroup events);

    /**
     * This method is invoked if the shouldAggregate method is called and
     * returns true. Once this method returns an aggregated message the event
     * group is removed from the router
     * 
     * @param events the event group for this request
     * @return an aggregated message
     * @throws RoutingException if the aggregation fails. in this scenario the
     *             whole event group is removed and passed to the exception
     *             handler for this componenet
     */
    protected abstract UMOMessage aggregateEvents(EventGroup events) throws RoutingException;
}
