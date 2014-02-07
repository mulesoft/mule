/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.support;

import org.mule.api.MuleEvent;
import org.mule.module.cxf.CxfConstants;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 * This interceptor is responsible from compressing a message when
 * the Content-Encoding is set to gzip or x-gzip.
 * It won't set the outbound property since it requires that it's
 * previously set. This makes sense since we are proxying.
 */
public class ProxyGZIPOutInterceptor extends AbstractProxyGZIPInterceptor
{
    public ProxyGZIPOutInterceptor()
    {
        super(Phase.PREPARE_SEND);
        addAfter(MessageSenderInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) throws Fault
    {
        MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);

        if (event == null || event.getMessage() == null)
        {
            return;
        }

        if(isEncoded(event.getMessage()))
        {
            OutputStream os = message.getContent(OutputStream.class);
            if (os == null)
            {
                return;
            }

            try
            {
                message.setContent(OutputStream.class, new GZIPOutputStream(os));
            }
            catch(IOException io)
            {
                throw new Fault(io);
            }
        }
    }

}
