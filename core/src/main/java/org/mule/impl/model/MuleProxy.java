/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.UMOEvent;
import org.mule.umo.UMOException;
import org.mule.umo.UMOImmutableDescriptor;
import org.mule.umo.lifecycle.Lifecycle;
import org.mule.util.queue.QueueSession;

import javax.resource.spi.work.Work;

/**
 * <code>MuleProxy</code> is a proxy to a UMO. It is a poolable object that
 * that can be executed in it's own thread.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public interface MuleProxy extends Work, Lifecycle
{

    /**
     * Sets the current event being processed
     *
     * @param event the event being processed
     */
    public void onEvent(QueueSession session, UMOEvent event);

    public ComponentStatistics getStatistics();

    public void setStatistics(ComponentStatistics stat);

    /**
     * Makes a synchronous call on the UMO
     *
     * @param event the event to pass to the UMO
     * @return the return event from the UMO
     * @throws UMOException if the call fails
     */
    public Object onCall(UMOEvent event) throws UMOException;
    /**
     * When an exception occurs this method can be called to invoke the
     * configured UMOExceptionStrategy on the UMO
     * 
     * @param exception If the UMOExceptionStrategy implementation fails
     */
    public void handleException(Exception exception);


    /**
     * Determines if the proxy is suspended
     * 
     * @return true if the proxy (and the UMO) are suspended
     */
    public boolean isSuspended();

    /**
     * Controls the suspension of the UMO event processing
     */
    public void suspend();

    /**
     * Triggers the UMO to resume processing of events if it is suspended
     */
    public void resume();


    /*
     * (non-Javadoc)
     * 
     * @see org.mule.umo.UMOLifecycleAdapter#getDescriptor()
     */
    public UMOImmutableDescriptor getDescriptor();
}
