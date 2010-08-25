/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport;

import org.mule.api.lifecycle.LifecycleException;
import org.mule.config.i18n.Message;

/** 
 * When this exception is thrown it will trigger a retry (reconnection) policy to go into effect if one is configured.
 */
public class ConnectException extends LifecycleException
{
    /** Serial version */
    private static final long serialVersionUID = -7802483584780922653L;

    public ConnectException(Message message, Object failed)
    {
        super(message, failed);
    }

    public ConnectException(Message message, Throwable cause, Object failed)
    {
        super(message, cause, failed);
    }

    public ConnectException(Throwable cause, Object failed)
    {
        super(cause, failed);
    }
}
