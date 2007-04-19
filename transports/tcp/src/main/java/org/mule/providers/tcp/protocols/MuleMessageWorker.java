/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import org.mule.impl.MuleMessage;
import org.mule.impl.RequestContext;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang.SerializationUtils;

/**
 * Helper class for Mule message handling so that we can apply the same logic across all
 * sub-protocols (default, EOF and length).
 */
class MuleMessageWorker
{

    private MuleMessageWorker()
    {
        // no-op
    }

    public static void doWrite(OutputStream os) throws IOException
    {
        MuleMessage msg = (MuleMessage) RequestContext.getEvent().getMessage();
        byte[] data = SerializationUtils.serialize(msg);
        os.write(data);
    }

    public static Object doRead(Object message) throws IOException
    {
        byte[] tmp = (byte[]) message;

        if (tmp == null)
        {
            return null;
        }
        else
        {
            return SerializationUtils.deserialize(tmp);
        }
    }

}
