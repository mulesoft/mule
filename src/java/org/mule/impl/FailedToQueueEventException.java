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
package org.mule.impl;

import org.mule.config.i18n.Message;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;

/**
 * <code>FailedToQueueEventException</code> is thrown when an event cannot be
 * put on an internal component queue.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class FailedToQueueEventException extends ComponentException
{
    public FailedToQueueEventException(Message message, UMOMessage umoMessage, UMOComponent component)
    {
        super(message, umoMessage, component);
    }

    public FailedToQueueEventException(Message message, UMOMessage umoMessage, UMOComponent component, Throwable cause)
    {
        super(message, umoMessage, component, cause);
    }

    public FailedToQueueEventException(UMOMessage umoMessage, UMOComponent component, Throwable cause)
    {
        super(umoMessage, component, cause);
    }
}
