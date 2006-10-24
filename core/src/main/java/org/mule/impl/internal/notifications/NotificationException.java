/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.internal.notifications;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;

/**
 * Thrown by the ServerNotification Manager it unrecognised listeners or events are
 * passed to the manager
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class NotificationException extends UMOException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -5998352122311445746L;

    /**
     * @param message the exception message
     */
    public NotificationException(Message message)
    {
        super(message);
    }

    /**
     * @param message the exception message
     * @param cause the exception that cause this exception to be thrown
     */
    public NotificationException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
