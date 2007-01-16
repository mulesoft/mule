/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MPL style
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import org.mule.providers.tcp.TcpProtocol;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.lang.SerializationUtils;

/**
 * This Abstract class has been introduced so as to have the byte protocols (i.e. the
 * protocols that had only a single write method taking just an array of bytes as a
 * parameter) to inherit from since they will all behave the same, i.e. if the object
 * is serializable, serialize it into an array of bytes and send it.
 */
public abstract class ByteProtocol implements TcpProtocol
{
    public void write(OutputStream os, Serializable data) throws IOException
    {
        if (data instanceof byte[])
        {
            write(os, (byte[])data);
        }
        else
        {
            write(os, SerializationUtils.serialize(data));
        }
    }
}
