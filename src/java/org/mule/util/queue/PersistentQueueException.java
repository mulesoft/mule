/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.util.queue;

import org.mule.MuleRuntimeException;

/**
 * <code>PersistentQueueException</code> is thrown when a Mule persistent
 * queue cannot save or remove an item or if there is a problem loading the
 * queue
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class PersistentQueueException extends MuleRuntimeException
{
    public PersistentQueueException(String message)
    {
        super(message);
    }

    public PersistentQueueException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
