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

import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.apache.commons.lang.SerializationUtils;

/**
 * This Protocol will send the actual Mule Message over the TCP channel, and in this
 * way we are preserving any headers which might be needed, for example Correlation
 * IDs in order to be able to aggregate messages after chunking.
 */
public class MuleMessageProtocol extends DefaultProtocol
{
    public Serializable read(InputStream is) throws IOException
    {
        byte[] tmp = (byte[])super.read(is);

        if (tmp == null)
        {
            return null;
        }
        else
        {
            return (MuleMessage)SerializationUtils.deserialize(tmp);
        }
    }

    public void write(OutputStream os, byte[] data) throws IOException
    {
        MuleMessage msg = (MuleMessage)RequestContext.getEvent().getMessage();
        data = SerializationUtils.serialize(msg);
        super.write(os, data);
    }
}
