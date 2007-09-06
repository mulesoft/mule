/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.umo.endpoint;

import org.mule.MuleRuntimeException;
import org.mule.config.i18n.Message;

public class InvalidEndpointTypeException extends MuleRuntimeException
{

    private static final long serialVersionUID = 8597088580804178563L;

    public InvalidEndpointTypeException(Message message)
    {
        super(message);
    }

    public InvalidEndpointTypeException(Message message, Throwable cause)
    {
        super(message, cause);
    }

}


