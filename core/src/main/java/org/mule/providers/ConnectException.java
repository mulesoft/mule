/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.config.i18n.Message;
import org.mule.umo.lifecycle.LifecycleException;
import org.mule.umo.provider.UMOConnectable;

/** TODO document */
public class ConnectException extends LifecycleException
{
    /** Serial version */
    private static final long serialVersionUID = -7802483584780922653L;

    public ConnectException(Message message, UMOConnectable component)
    {
        super(message, component);
    }

    public ConnectException(Message message, Throwable cause, UMOConnectable component)
    {
        super(message, cause, component);
    }

    public ConnectException(Throwable cause, UMOConnectable component)
    {
        super(cause, component);
    }
}
