/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
        
        String etag = message.getStringProperty(HttpConstants.HEADER_IF_NONE_MATCH, null);
        if ((etag != null) && etag.equals(ETAG_VALUE))
        {
            message = new DefaultMuleMessage(StringUtils.EMPTY, eventContext.getMuleContext());
            message.setIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, HttpConstants.SC_NOT_MODIFIED);
        }
        
        message.setProperty(HttpConstants.HEADER_ETAG, ETAG_VALUE);        
        return message;
    }
}


