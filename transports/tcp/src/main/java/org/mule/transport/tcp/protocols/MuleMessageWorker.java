/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

import org.mule.DefaultMuleMessage;
import org.mule.RequestContext;
import org.mule.api.MuleException;
import org.mule.api.transformer.wire.WireFormat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Helper class for Mule message handling so that we can apply the same logic across all
 * sub-protocols (default, EOF and length).
 */
class MuleMessageWorker
{

    private final WireFormat wireFormat;

    MuleMessageWorker(WireFormat wireFormat)
    {
        this.wireFormat = wireFormat;
    }

    public byte[] doWrite() throws IOException
    {
        //TODO fix the api here so there is no need to use the RequestContext
        DefaultMuleMessage msg = (DefaultMuleMessage) RequestContext.getEvent().getMessage();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try
        {
            wireFormat.write(baos, msg, msg.getEncoding());
        }
        catch (MuleException e)
        {
            throw new IOException(e.getDetailedMessage());
        }
        return baos.toByteArray();
    }

    public Object doRead(Object message) throws IOException
    {
        if (message == null)
        {
            return null;
        }

        InputStream in;
        if (message instanceof byte[])
        {
            in = new ByteArrayInputStream((byte[]) message);
        }
        else
        {
            in = (InputStream) message;
        }

        try
        {
            return wireFormat.read(in);
        }
        catch (MuleException e)
        {
            throw new IOException(e.getDetailedMessage());
        }

    }

}
