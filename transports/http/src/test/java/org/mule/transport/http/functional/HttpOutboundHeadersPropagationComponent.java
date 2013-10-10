/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.functional;

import java.util.Map;
import java.util.TreeMap;

import org.mule.api.MuleEventContext;
import org.mule.api.MuleMessage;
import org.mule.api.lifecycle.Callable;
import org.mule.transport.http.CookieHelper;

import org.apache.commons.httpclient.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpOutboundHeadersPropagationComponent implements Callable
{

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    public Object onCall(MuleEventContext muleEventContext) throws Exception
    {
        MuleMessage m = muleEventContext.getMessage();
        Map<String, Object> headers = new TreeMap<String, Object>();
        for(String s : m.getInboundPropertyNames())
        {
            headers.put(s, m.getInboundProperty(s));
        }
        return headers;
    }
}
