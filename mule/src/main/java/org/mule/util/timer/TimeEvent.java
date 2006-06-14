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
 *
 */
package org.mule.util.timer;

import java.util.EventObject;

/**
 * <code>TimeEvent</code> TODO is an event that occurs at a specified number
 * of milliseconds.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class TimeEvent extends EventObject
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -7540426406525372393L;

    /**
     * The event name
     */
    private String name;

    /**  */
    private long timeExpired;

    /**
     * @param source
     * @param name
     * @param timeExpired
     */
    public TimeEvent(Object source, String name, long timeExpired)
    {
        super(source);
        this.name = name;
        this.timeExpired = timeExpired;
    }

    /**
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * @return
     */
    public long getTimeExpired()
    {
        return timeExpired;
    }
}
