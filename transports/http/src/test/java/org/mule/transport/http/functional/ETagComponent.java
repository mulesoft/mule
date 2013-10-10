/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.util.StringUtils;

public class ETagComponent implements org.mule.api.lifecycle.Callable
{
    private static String ETAG_VALUE = "0123456789";
    
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        MuleMessage message = eventContext.getMessage();

        String etag = message.getOutboundProperty(HttpConstants.HEADER_IF_NONE_MATCH);
        if ((etag != null) && etag.equals(ETAG_VALUE))
        {
            message = new DefaultMuleMessage(StringUtils.EMPTY, eventContext.getMuleContext());
            message.setOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, HttpConstants.SC_NOT_MODIFIED);
        }
        
        message.setOutboundProperty(HttpConstants.HEADER_ETAG, ETAG_VALUE);        
        return message;
    }
}


