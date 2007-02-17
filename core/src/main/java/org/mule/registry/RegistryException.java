/*
 * $Id: $
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry;

import org.mule.config.i18n.Message;
import org.mule.umo.UMOException;

/**
 * Any registry-related exception: unable to register/deregister an entity, etc.
 */
public class RegistryException extends UMOException
{
    public RegistryException(Message message)
    {
        super(message);
    }

    public RegistryException(Message message, Throwable cause)
    {
        super(message, cause);
    }
}
