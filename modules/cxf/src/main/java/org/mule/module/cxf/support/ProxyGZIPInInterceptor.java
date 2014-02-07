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
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 * This interceptor is responsible for decompressing a message
 * received with Content-Encoding that includes gzip or x-zip.
 * It won't delete the Content-Encoding property since we are proxying
 * and we might still require it.
 */
public class ProxyGZIPInInterceptor extends AbstractProxyGZIPInterceptor
{
    public ProxyGZIPInInterceptor()
    {
        super(Phase.RECEIVE);
        addBefore(AttachmentInInterceptor.class.getName());
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
            InputStream is = message.getContent(InputStream.class);
            if (is == null)
            {
                return;
            }

            try
            {
                GZIPInputStream zipInput = new GZIPInputStream(is);
                message.setContent(InputStream.class, zipInput);
            }
            catch(IOException io)
            {
                throw new Fault(io);
            }
        }
    }
}
