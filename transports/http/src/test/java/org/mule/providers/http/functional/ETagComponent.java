/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http.functional;

import org.mule.impl.MuleMessage;
import org.mule.providers.DefaultMessageAdapter;
import org.mule.providers.http.HttpConnector;
import org.mule.providers.http.HttpConstants;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOMessage;

public class ETagComponent implements org.mule.umo.lifecycle.Callable
{
    String ETAG_VALUE = "0123456789";
    
    public Object onCall(UMOEventContext eventContext) throws Exception
    {
        UMOMessage msg = eventContext.getMessage();
        
        String etag = msg.getStringProperty(HttpConstants.HEADER_IF_NONE_MATCH, null);
        if (etag != null && etag.equals(ETAG_VALUE))
        {
           DefaultMessageAdapter res = new DefaultMessageAdapter("");
           res.setIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 304);
           msg = new MuleMessage(res);
        }
        
        msg.setProperty(HttpConstants.HEADER_ETAG, ETAG_VALUE);
        
        return msg;
    }

}


