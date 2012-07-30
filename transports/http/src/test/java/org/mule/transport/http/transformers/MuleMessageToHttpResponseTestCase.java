/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.http.transformers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleMessage;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.HttpResponse;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.junit.Assert;
import org.junit.Test;

@SmallTest
public class MuleMessageToHttpResponseTestCase
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
}
