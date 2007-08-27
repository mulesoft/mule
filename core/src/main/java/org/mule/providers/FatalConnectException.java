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
import org.mule.umo.lifecycle.FatalException;
import org.mule.umo.provider.UMOConnectable;

/** TODO document */
public class FatalConnectException extends FatalException
{
    /** Serial version */
    private static final long serialVersionUID = 3300563235465630595L;

    public FatalConnectException(Message message, UMOConnectable component)
    {
        super(message, component);
    }

    public FatalConnectException(Message message, Throwable cause, UMOConnectable component)
    {
        super(message, cause, component);
    }

    public FatalConnectException(Throwable cause, UMOConnectable component)
    {
        super(cause, component);
    }
}
