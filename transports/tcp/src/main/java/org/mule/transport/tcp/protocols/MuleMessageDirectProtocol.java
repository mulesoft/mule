/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.tcp.protocols;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.transformer.wire.SerializedMuleMessageWireFormat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This Protocol will send the actual Mule Message over the TCP channel, and in this
 * way we are preserving any headers which might be needed, for example Correlation
 * IDs in order to be able to aggregate messages after chunking.  Data are read until
 * no more are (momentarily) available.
 */
public class MuleMessageDirectProtocol extends DirectProtocol implements MuleContextAware
{

    private final SerializedMuleMessageWireFormat wireFormat = new SerializedMuleMessageWireFormat();
    private final MuleMessageWorker messageWorker = new MuleMessageWorker(wireFormat);

    @Override
    public Object read(InputStream is) throws IOException
    {
        return messageWorker.doRead(super.read(is));
    }

    @Override
    public void write(OutputStream os, Object data) throws IOException
    {
        super.write(os, messageWorker.doWrite());
    }

    @Override
    public void setMuleContext(MuleContext context)
    {
        wireFormat.setMuleContext(context);
    }
}
