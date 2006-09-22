/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * <code>EventTimerTask</code> is a task that causes TimeEvent to be fired to
 * listening objects when a specific number of milliseconds have passed. This
 * implementation is based on the java.util.TimerTask.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class EventTimerTask extends TimerTask
{
    /**
     * A list of listeners on this task
     */
    private List listeners = null;

    /**
     * The name of the task
     */
    private String name = null;

    /**
     * Determines if the task has been started
     */
    private boolean started = true;

    /**
     * Constructs a EventTimeTask and registers a listener with it
     * 
     * @param listener the listener to register
     */
    public EventTimerTask(TimeEventListener listener)
    {
        super();
        addListener(listener);
        this.name = "EventTimerTask." + hashCode();
    }

    /**
     * Constructs a EventTimeTask and registers a listener with it
     * 
     * @param listener the listener to register
     * @param name the name for the task
     */
    public EventTimerTask(TimeEventListener listener, String name)
    {
        super();
        addListener(listener);
        this.name = name;
    }

    /**
     * The action to be performed by this timer task. The fireTime event method
     * is called.
     */
    public void run()
    {

        TimeEvent event = new TimeEvent(this, getName(), scheduledExecutionTime());
        fireTimerEvent(event);
    }

    /**
     * Gets the task name (this is also the timer thread name)
     * 
     * @return the task name
     */
    public String getName()
    {
        return name;
    }

    public void removeListener(TimeEventListener listener)
    {
        if (listeners != null && listeners.contains(listener)) {
            listeners.remove(listener);
        }
    }

    public void removeAllListeners()
    {
        listeners = new ArrayList();
    }

    public void addListener(TimeEventListener listener)
    {
        if (listeners == null) {
            listeners = new ArrayList();
            listeners.add(listener);
        } else if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    protected void fireTimerEvent(TimeEvent event)
    {
        if (listeners != null && started) {
            int count = listeners.size();
            for (int i = 0; i < count; i++) {
                ((TimeEventListener) listeners.get(i)).timeExpired(event);
            }
        }
    }

    public void stop()
    {
        started = false;
    }

    public void start()
    {
        started = true;
    }

    public boolean isStarted()
    {
        return started;
    }
}
