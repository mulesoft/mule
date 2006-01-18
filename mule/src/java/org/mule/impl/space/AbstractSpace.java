/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.impl.space;

import edu.emory.mathcs.backport.java.util.concurrent.CopyOnWriteArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.MuleManager;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.space.UMOSpace;
import org.mule.umo.space.UMOSpaceEvent;
import org.mule.umo.space.UMOSpaceEventListener;
import org.mule.umo.space.UMOSpaceException;

import java.util.Iterator;
import java.util.List;

/**
 * Provides core functionality for all spaces, including listenr management and Server notification support.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public abstract class AbstractSpace implements UMOSpace {

    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    protected List listeners = new CopyOnWriteArrayList();
    protected List moniterListeners = new CopyOnWriteArrayList();
    protected String name;
    protected boolean enableMonitorEvents = true;
    protected UMOTransactionFactory transactionFactory = null;

    protected AbstractSpace(String name) {
        this.name = name;
        fireMonitorEvent(SpaceMonitorNotification.SPACE_CREATED, this);
    }

    protected AbstractSpace(String name, boolean enableMonitorEvents) {
        this(name);
        this.enableMonitorEvents = enableMonitorEvents;
    }

    public void addListener(UMOSpaceEventListener listener) {
        listeners.add(listener);
        fireMonitorEvent(SpaceMonitorNotification.SPACE_LISTENER_ADDED, listener);
    }

    public void removeListener(UMOSpaceEventListener listener) {
        listeners.remove(listener);
        fireMonitorEvent(SpaceMonitorNotification.SPACE_LISTENER_REMOVED, listener);
    }

    public void addMonitorListener(SpaceMonitorNotificationListener listener) {
        if(enableMonitorEvents==false) {
            logger.warn("Space monitor notifications for " + name + " space are currently disabled");
        }
        moniterListeners.add(listener);
    }

    public void removeMonitorListener(SpaceMonitorNotificationListener listener) {
        listeners.remove(listener);
    }

    public String getName() {
        return name;
    }

    public final void put(Object value)  throws UMOSpaceException {
        doPut(value);
        fireListeners();
        fireMonitorEvent(SpaceMonitorNotification.SPACE_ITEM_ADDED, value);
    }

    public void put(Object value, long lease) throws UMOSpaceException {
        if(logger.isTraceEnabled()) logger.trace("Writing value to space: " + name + ", with lease: " + lease + ", Value is: " + value );
        doPut(value, lease);
        fireListeners();
        fireMonitorEvent(SpaceMonitorNotification.SPACE_ITEM_ADDED, value);

    }

    public Object take() throws UMOSpaceException {
        Object item = doTake();

        if(item==null) {
            if(logger.isTraceEnabled()) logger.trace("Taking from space: " + name + " returned null");
            fireMonitorEvent(SpaceMonitorNotification.SPACE_ITEM_MISS, item);
        } else {
            if(logger.isTraceEnabled()) logger.trace("Taking from space: " + name + " returned:" + item);
            fireMonitorEvent(SpaceMonitorNotification.SPACE_ITEM_REMOVED, item);
        }
        return item;
    }

    public Object take(long timeout) throws UMOSpaceException {
        Object item = doTake(timeout);
        if(item==null) {
            if(logger.isTraceEnabled()) logger.trace("Taking from space (timeout "+ timeout + "): returned null");            
            fireMonitorEvent(SpaceMonitorNotification.SPACE_ITEM_MISS, item);
        } else {
            fireMonitorEvent(SpaceMonitorNotification.SPACE_ITEM_REMOVED, item);
            if(logger.isTraceEnabled()) logger.trace("Taking from space (timeout "+ timeout + "): " + name + " returned:" + item);
        }
        return item;
    }

    public Object takeNoWait() throws UMOSpaceException {
        Object item = doTakeNoWait();
        if(item==null) {
            fireMonitorEvent(SpaceMonitorNotification.SPACE_ITEM_MISS, item);
            if(logger.isTraceEnabled()) logger.trace("Taking from space (no wait): " + name + " returned: null");
        } else {
            fireMonitorEvent(SpaceMonitorNotification.SPACE_ITEM_REMOVED, item);
            if(logger.isTraceEnabled()) logger.trace("Taking from space (no wait): " + name + " returned:" + item);
        }
        return item;
    }

    protected void fireListeners() {
        if(listeners.size() > 0) {
            Object item = null;
            try {
                item = takeNoWait();
            } catch (UMOSpaceException e) {
                logger.error(e.getMessage(), e);
            }
            if(item==null) {
                logger.warn("Item was taken before listeners could be updated, try using a different type of space");
                return;
            }
            for (Iterator iterator = listeners.iterator(); iterator.hasNext();) {
                UMOSpaceEventListener spaceEventListener = (UMOSpaceEventListener) iterator.next();
                spaceEventListener.onEvent(new UMOSpaceEvent(item, this));
            }
        }
    }

    protected void fireMonitorEvent(int action, Object item) {
        if(enableMonitorEvents) {
            MuleManager.getInstance().fireNotification(new SpaceMonitorNotification(this, action, item));
        }
    }

    public void dispose() {
        doDispose();
        fireMonitorEvent(SpaceMonitorNotification.SPACE_DISPOSED, this);
    }

    public UMOTransactionFactory getTransactionFactory() {
        return transactionFactory;
    }

    public void setTransactionFactory(UMOTransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
    }

    public abstract void doPut(Object value) throws UMOSpaceException;

    public abstract void doPut(Object value, long lease) throws UMOSpaceException;

    public abstract Object doTake() throws UMOSpaceException;

    public abstract Object doTake(long timeout) throws UMOSpaceException;

    public abstract Object doTakeNoWait() throws UMOSpaceException ;

    public abstract void doDispose();
}
