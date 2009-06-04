/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.transformers;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractMessageAwareTransformer;
import org.mule.transport.servlet.HttpRequestMessageAdapter;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Returns a simple Map of the parameters sent with the HTTP Request.  
 * If the same parameter is given more than once, only the first value for it will be in the Map.
 */
public class HttpRequestToParameterMap extends AbstractMessageAwareTransformer
{
    public HttpRequestToParameterMap()
    {
        registerSourceType(Object.class);
        setReturnClass(Map.class);
    }

    public Object transform(MuleMessage message, String outputEncoding) throws TransformerException
    {
        HttpRequestMessageAdapter messageAdapter = (HttpRequestMessageAdapter) message.getAdapter();
        Enumeration<String> paramNames = messageAdapter.getParameterNames();
        
        Map <String, String> parameters = new HashMap();
        String paramName;
        while (paramNames.hasMoreElements())
        {
            paramName = paramNames.nextElement();            
            parameters.put(paramName, messageAdapter.getParameterValues(paramName)[0]);
        }
        return parameters;
    }
}
