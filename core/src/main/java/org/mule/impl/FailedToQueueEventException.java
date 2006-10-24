/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl;

import org.mule.config.i18n.Message;
import org.mule.umo.ComponentException;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMOMessage;

/**
 * <code>FailedToQueueEventException</code> is thrown when an event cannot be put
 * on an internal component queue.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class FailedToQueueEventException extends ComponentException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8368283988424746098L;

    public FailedToQueueEventException(Message message, UMOMessage umoMessage, UMOComponent component)
    {
        super(message, umoMessage, component);
    }

    public FailedToQueueEventException(Message message,
                                       UMOMessage umoMessage,
                                       UMOComponent component,
                                       Throwable cause)
    {
        super(message, umoMessage, component, cause);
    }

    public FailedToQueueEventException(UMOMessage umoMessage, UMOComponent component, Throwable cause)
    {
        super(umoMessage, component, cause);
    }
}
