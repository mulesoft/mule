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
package org.mule.umo.provider;

import org.mule.MuleRuntimeException;

/**
 * <code>UniqueIdNotSupportedException</code> is thrown by UMOMessageAdapter.getUniqueId()
 * if the underlying message does not support or have a unique identifier.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public class UniqueIdNotSupportedException extends MuleRuntimeException
{
    public UniqueIdNotSupportedException(UMOMessageAdapter adapter)
    {
        super("Apater: " + adapter.getClass().getName() + " does not support unique identifiers");
    }

    public UniqueIdNotSupportedException(UMOMessageAdapter adapter, String message)
    {
        super("Apater: " + adapter.getClass().getName() + " does not support unique identifiers: " + message);
    }

    public UniqueIdNotSupportedException(UMOMessageAdapter adapter, Throwable cause)
    {
        super("Apater: " + adapter.getClass().getName() + " does not support unique identifiers: " + cause.getMessage(), cause);
    }

}
