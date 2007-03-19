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
import org.mule.umo.provider.UMOMessageAdapter;

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

    public void write(OutputStream os, Object data) throws IOException
    {
        // By default the UMOMessageAdapter object itself is passed in, I guess so
        // that the whole adapter can be serialised if necessary. I'm doing the check
        // here to extract the real payload, rather than extracting it in the
        // TcpMessageReceiver where the protocol is called
        if (data instanceof UMOMessageAdapter)
        {
            data = ((UMOMessageAdapter)data).getPayload();
        }

        if (data instanceof byte[])
        {
            write(os, (byte[])data);
        }
        else if (data instanceof String)
        {
            // TODO SF: encoding is lost/ignored; it is probably a good idea to have
            // a separate "stringEncoding" property on the protocol
            write(os, ((String)data).getBytes());
        }
        else if (data instanceof Serializable)
        {
            write(os, SerializationUtils.serialize((Serializable)data));
        }
        else
        {
            // TODO SF: Throw Exception since have no idea how to serialize!!! Which
            // exception should we throw? (HH) how about IllegalArgumentException?
        }
    }

}
