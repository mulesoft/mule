/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.cxf.support;

import static com.google.common.net.HttpHeaders.CONTENT_TYPE;
import static org.mule.runtime.module.http.api.HttpConstants.Methods.POST;
import static org.mule.runtime.module.http.api.HttpConstants.RequestProperties.HTTP_METHOD_PROPERTY;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;

import org.mule.runtime.core.NonBlockingVoidMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.module.cxf.CxfConstants;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class MuleProtocolHeadersOutInterceptor
    extends AbstractPhaseInterceptor<Message>
{

    private static final Log logger = LogFactory.getLog(MuleProtocolHeadersOutInterceptor.class);

    public MuleProtocolHeadersOutInterceptor()
    {
        super(Phase.PRE_STREAM);
        getAfter().add(AttachmentOutInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault
    {
        MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);
        
        if (event == null || event instanceof NonBlockingVoidMuleEvent)
        {
            return;
        }

        MuleMessage muleMsg = event.getMessage();
        
        if (muleMsg == null)
        {
            return;
        }
        extractAndSetContentType(message, muleMsg);
        extractAndSet(message, muleMsg, Message.RESPONSE_CODE, HTTP_STATUS_PROPERTY);

        String method = (String) message.get(Message.HTTP_REQUEST_METHOD);
        if (method == null)
        {
            method = POST.name();
        }

        muleMsg.setOutboundProperty(HTTP_METHOD_PROPERTY, method);
        
        Map<String, List<String>> reqHeaders = CastUtils.cast((Map<?, ?>) message.get(Message.PROTOCOL_HEADERS));
        if (reqHeaders != null)
        {
            for (Map.Entry<String, List<String>> e : reqHeaders.entrySet())
            {
                String key = e.getKey();
                String val = format(e.getValue());

                muleMsg.setOutboundProperty(key, val);
            }
        }   
        
        if (!Boolean.TRUE.equals(message.containsKey(Message.REQUESTOR_ROLE)))
        {
            message.getInterceptorChain().pause();
        }
    }

    private void extractAndSet(Message message, MuleMessage muleMsg, String cxfHeader, String muleHeader)
    {
        if(message.get(cxfHeader) instanceof Serializable)
        {
            Serializable val = (Serializable) message.get(cxfHeader);
            if (val != null)
            {
                muleMsg.setOutboundProperty(muleHeader, val);
            }
        }
        else
        {
            logger.warn("The header " + cxfHeader + "is not serializable and will not be propagated by Mule");
        }
    }

    private void extractAndSetContentType(Message message, MuleMessage muleMsg)
    {
        String ct = (String) message.get(Message.CONTENT_TYPE);
        if (ct != null)
        {
            String encoding = getEncoding(message);
            if (ct.indexOf("charset") == -1)
            {
                ct = ct + "; charset=" + encoding;
            }
            muleMsg.setOutboundProperty(CONTENT_TYPE, ct);
        }
    }

    private String getEncoding(Message message)
    {
        Exchange ex = message.getExchange();
        String encoding = (String)message.get(Message.ENCODING);
        if (encoding == null && ex.getInMessage() != null) {
            encoding = (String) ex.getInMessage().get(Message.ENCODING);
            message.put(Message.ENCODING, encoding);
        }

        if (encoding == null) {
            encoding = "UTF-8";
            message.put(Message.ENCODING, encoding);
        }
        return encoding;
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


