/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.MuleMessage;
import org.mule.api.service.Service;
import org.mule.api.service.ServiceException;
import org.mule.config.i18n.Message;

/**
 * <code>FailedToQueueEventException</code> is thrown when an event cannot be put
 * on an internal service queue.
 */

public class FailedToQueueEventException extends ServiceException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = -8368283988424746098L;

    public FailedToQueueEventException(Message message, MuleMessage muleMessage, Service service)
    {
        super(message, muleMessage, service);
    }

    public FailedToQueueEventException(Message message,
                                       MuleMessage muleMessage,
                                       Service service,
                                       Throwable cause)
    {
        super(message, muleMessage, service, cause);
    }

    public FailedToQueueEventException(MuleMessage muleMessage, Service service, Throwable cause)
    {
        super(muleMessage, service, cause);
    }
}
