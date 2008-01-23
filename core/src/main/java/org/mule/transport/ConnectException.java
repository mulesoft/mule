/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.transport.Connectable;
import org.mule.config.i18n.Message;

/** TODO document */
public class ConnectException extends LifecycleException
{
    /** Serial version */
    private static final long serialVersionUID = -7802483584780922653L;

    public ConnectException(Message message, Connectable component)
    {
        super(message, component);
    }

    public ConnectException(Message message, Throwable cause, Connectable component)
    {
        super(message, cause, component);
    }

    public ConnectException(Throwable cause, Connectable component)
    {
        super(cause, component);
    }
}
