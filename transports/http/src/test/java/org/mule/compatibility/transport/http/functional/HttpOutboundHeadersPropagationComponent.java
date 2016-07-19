/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.functional;

import org.mule.runtime.core.api.MuleEventContext;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.lifecycle.Callable;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpOutboundHeadersPropagationComponent implements Callable
{

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Object onCall(MuleEventContext muleEventContext) throws Exception
    {
        MuleMessage m = muleEventContext.getMessage();
        Map<String, Object> headers = new TreeMap<>();
        for(String s : m.getInboundPropertyNames())
        {
            headers.put(s, m.getInboundProperty(s));
        }
        headers.put("Content-Type", m.getDataType().getMediaType().toRfcString());
        return headers;
    }
}
