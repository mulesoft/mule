/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.remoting;

import org.mule.module.client.i18n.ClientMessages;

/**
 * Exception is thrown when the server is using a wire format that the client does not support
 */
public class UnsupportedWireFormatException extends RemoteDispatcherException
{
    /**
     * @param message the exception message
     */
    public UnsupportedWireFormatException(String wireFormat, Exception e)
    {
        super(ClientMessages.unsupportedServerWireForat(wireFormat), e);
    }
}
