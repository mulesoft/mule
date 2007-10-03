/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This Protocol will send the actual Mule Message over the TCP channel, and in this
 * way we are preserving any headers which might be needed, for example Correlation
 * IDs in order to be able to aggregate messages after chunking.  Data are read until
 * no more are (momentarily) available.
 */
public class MuleMessageDirectProtocol extends DirectProtocol
{

    // @Override
    public Object read(InputStream is) throws IOException
    {
        return MuleMessageWorker.doRead(super.read(is));
    }

    // @Override
    public void write(OutputStream os, byte[] data) throws IOException
    {
        super.write(os, MuleMessageWorker.doWrite());
    }

}
