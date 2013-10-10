/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http.transformers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.transport.http.HttpResponse;

import java.util.HashSet;
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
}
