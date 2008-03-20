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

import static org.mule.api.config.MuleProperties.MULE_EVENT_PROPERTY;

import org.mule.api.MuleEvent;

import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor.MessageSenderEndingInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class MuleProtocolHeadersOutInterceptor
    extends AbstractPhaseInterceptor<Message>
{

    public MuleProtocolHeadersOutInterceptor()
    {
        super(Phase.PREPARE_SEND_ENDING);
        getBefore().add(MessageSenderEndingInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault
    {
        MuleEvent event = (MuleEvent) message.getExchange().get(MULE_EVENT_PROPERTY);

        if (event == null)
        {
            return;
        }
        
        Map<String, List<String>> reqHeaders = CastUtils.cast((Map<?,?>)message.get(Message.PROTOCOL_HEADERS));
        if (reqHeaders != null) {
            for (Map.Entry<String, List<String>> e : reqHeaders.entrySet()) {
                event.getMessage().setProperty(e.getKey(), format(e.getValue()));
            }
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


