/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.junit.Assert;
import org.junit.Test;

@SmallTest
public class MuleMessageToHttpResponseTestCase extends AbstractMuleTestCase
{

    @Test
    public void testSetCookieOnOutbound() throws Exception
    {
        MuleMessageToHttpResponse transformer = new MuleMessageToHttpResponse();
        MuleMessage msg = mock(MuleMessage.class);

        Cookie[] cookiesOutbound = new Cookie[2];
        cookiesOutbound[0] = new Cookie("domain", "name-out-1", "value-out-1");
        cookiesOutbound[1] = new Cookie("domain", "name-out-2", "value-out-2");

        when(msg.getOutboundProperty("Set-Cookie")).thenReturn(cookiesOutbound);
        Set props = new HashSet();
        props.add("Set-Cookie");
        when(msg.getOutboundPropertyNames()).thenReturn(props);

        HttpResponse response = transformer.createResponse(null, "UTF-8", msg);
        Header[] headers = response.getHeaders();
        int cookiesSet = 0;
        for(Header header : headers)
        {
            if ("Set-Cookie".equals(header.getName()))
            {
                cookiesSet++;
            }
        }
        Assert.assertEquals(cookiesOutbound.length, cookiesSet);
    }
    

    @Test
    public void testSetDateOnOutbound() throws Exception
    {
        MuleMessageToHttpResponse transformer = new MuleMessageToHttpResponse();
        MuleMessage msg = mock(MuleMessage.class);

        HttpResponse response = transformer.createResponse(null, "UTF-8", msg);
        Header[] headers = response.getHeaders();

        boolean hasDateHeader = false;
        for (Header header : headers)
        {
            if (HttpConstants.HEADER_DATE.equals(header.getName()))
            {
                hasDateHeader = true;
                // validate that the header is in the appropriate format (rfc-1123)
                SimpleDateFormat formatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT, Locale.US);
                formatter.setLenient(false);
                try
                {
                    formatter.parse(header.getValue());
                }
                catch (ParseException e)
                {
                    // will to accept 24 hour style (which is really what it's supposed to be).
                    formatter.setLenient(true);
                    formatter.parse(header.getValue());
                    formatter = new SimpleDateFormat(HttpConstants.DATE_FORMAT.replaceFirst("hh", "HH"), Locale.US);
                    formatter.setLenient(false);
                    formatter.parse(header.getValue());
                }
            }
        }
        Assert.assertTrue("Missing 'Date' header", hasDateHeader);
    }
    
    @Test
    public void testContentTypeOnOutbound() throws Exception
    {
        MuleMessageToHttpResponse transformer = new MuleMessageToHttpResponse();
        final String contentType = "text/xml";
        final String wrongContentType = "text/json";
        Map<String, Object> outboundProperties =  new HashMap<String, Object>();
        outboundProperties.put(HttpConstants.HEADER_CONTENT_TYPE, wrongContentType);
        MuleContext muleContext = mock(MuleContext.class);
        MuleMessage msg = new DefaultMuleMessage(null,outboundProperties, muleContext);
        //Making sure that the outbound property overrides both invocation and inbound
        msg.setInvocationProperty(HttpConstants.HEADER_CONTENT_TYPE, wrongContentType);
        msg.setProperty(HttpConstants.HEADER_CONTENT_TYPE, wrongContentType, PropertyScope.INBOUND);
        
        msg.setOutboundProperty(HttpConstants.HEADER_CONTENT_TYPE, contentType);

        HttpResponse response = transformer.createResponse(null, "UTF-8", msg);
        Header[] headers = response.getHeaders();

        boolean hasContentTypeHeader = false;
        for (Header header : headers)
        {
            if (HttpConstants.HEADER_CONTENT_TYPE.equals(header.getName()))
            {
                hasContentTypeHeader = true;
                Assert.assertEquals(contentType, header.getValue());
            }
        }
        Assert.assertTrue("Missing "+HttpConstants.HEADER_CONTENT_TYPE+" header", hasContentTypeHeader);
    }
}
