/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers;

import org.mule.config.i18n.Message;
import org.mule.umo.lifecycle.FatalException;

/**
 * todo document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class FatalConnectException extends FatalException
{
    /**
     * Serial version
     */
    private static final long serialVersionUID = 3300563235465630595L;

    public FatalConnectException(Message message, Object component)
    {
        super(message, component);
    }

    public FatalConnectException(Message message, Throwable cause, Object component)
    {
        super(message, cause, component);
    }

    public FatalConnectException(Throwable cause, Object component)
    {
        super(cause, component);
    }
}
