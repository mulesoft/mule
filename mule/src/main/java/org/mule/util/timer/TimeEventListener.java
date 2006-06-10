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

import java.util.EventListener;

/**
 * <code>TimeEventListener</code> provides a method to pass timer events to
 * implementing objects.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface TimeEventListener extends EventListener
{
    /**
     * Passes the TimeEvent to an object
     * 
     * @param e the time event that occurred
     */
    void timeExpired(TimeEvent e);
}
