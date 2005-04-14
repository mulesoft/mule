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
package org.mule.util.queue;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Message;

/**
 * <code>PersistentQueueException</code> is thrown when a Mule persistent
 * queue cannot save or remove an item or if there is a problem loading the
 * queue
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class PersistentQueueException extends MuleRuntimeException
{
    public PersistentQueueException(Message message)
    {
        super(message);
    }

    public PersistentQueueException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
