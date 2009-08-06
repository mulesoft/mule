/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.support;

import org.mule.api.transport.MessageAdapter;
import org.mule.transport.cxf.CxfConstants;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class MuleProtocolHeadersOutInterceptor
    extends AbstractPhaseInterceptor<Message>
{

    public MuleProtocolHeadersOutInterceptor()
    {
        super(Phase.PRE_STREAM);
        getAfter().add(AttachmentOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault
    {
        MessageAdapter muleMsg = (MessageAdapter) message.getExchange().get(CxfConstants.MULE_MESSAGE);
        
        if (muleMsg == null)
        {
            return;
        }
        extractAndSet(message, muleMsg, Message.CONTENT_TYPE, HttpConstants.HEADER_CONTENT_TYPE);
        extractAndSet(message, muleMsg, Message.RESPONSE_CODE, HttpConnector.HTTP_STATUS_PROPERTY);

        String method = (String) message.get(Message.HTTP_REQUEST_METHOD);
        if (method == null) method = HttpConstants.METHOD_POST;
        
        muleMsg.setProperty(HttpConnector.HTTP_METHOD_PROPERTY, method);
        
        Map<String, List<String>> reqHeaders = CastUtils.cast((Map<?, ?>) message.get(Message.PROTOCOL_HEADERS));
        if (reqHeaders != null)
        {
            for (Map.Entry<String, List<String>> e : reqHeaders.entrySet())
            {
                String key = e.getKey();
                String val = format(e.getValue());
                
                muleMsg.setProperty(key, val);
            }
        }   
        
        if (!Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE)))
        {
            message.getInterceptorChain().pause();
        }
    }

    private void extractAndSet(Message message, MessageAdapter muleMsg, String cxfHeader, String muleHeader)
    {
        Object val = message.get(cxfHeader);
        if (val != null)
        {
            muleMsg.setProperty(muleHeader, val);
        }
    }

    private String format(List<String> value)
    {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        
        for (String s : value) {
            if (!first) 
            {
                sb.append(", ");
                first = false;
            }
            else 
            {
                first = false;
            }
            
            sb.append(s);
        }
        return sb.toString();
    }
}


