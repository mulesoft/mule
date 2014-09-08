/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.client.remoting;

import org.mule.module.client.i18n.ClientMessages;

/**
 * Exception is thrown when the server is using a wire format that the client does not support
 *
 * <b>Deprecated as of 3.6.0</b>
 */
@Deprecated
public class UnsupportedWireFormatException extends RemoteDispatcherException
{
    public UnsupportedWireFormatException(String wireFormat, Exception e)
    {
        super(ClientMessages.unsupportedServerWireForat(wireFormat), e);
    }
}
