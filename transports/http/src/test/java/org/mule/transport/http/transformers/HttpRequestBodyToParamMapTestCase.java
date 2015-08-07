/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.transformers;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mule.transport.http.HttpConstants.DEFAULT_CONTENT_TYPE;
import static org.mule.transport.http.HttpConstants.FORM_URLENCODED_CONTENT_TYPE;
import static org.mule.transport.http.HttpConstants.HEADER_CONTENT_TYPE;
import static org.mule.transport.http.HttpConstants.METHOD_GET;
import static org.mule.transport.http.HttpConstants.METHOD_POST;
import static org.mule.transport.http.HttpConstants.METHOD_PUT;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SmallTest
public class HttpRequestBodyToParamMapTestCase extends AbstractMuleTestCase
{

    @Mock(answer = RETURNS_DEEP_STUBS)
    private MuleContext muleContext;

    @Test
    public void validGet() throws TransformerException
    {
        MuleMessage msg = createMessage(METHOD_GET, DEFAULT_CONTENT_TYPE);
        verifyTransformation(transform(msg));
    }

    @Test
    public void validPost() throws TransformerException
    {
        MuleMessage msg = createMessage(METHOD_POST, FORM_URLENCODED_CONTENT_TYPE);
        verifyTransformation(transform(msg));
    }

    @Test
    public void validPut() throws TransformerException
    {
        MuleMessage msg = createMessage(METHOD_PUT, FORM_URLENCODED_CONTENT_TYPE);
        verifyTransformation(transform(msg));
    }

    @Test(expected = TransformerException.class)
    public void invalidContentType() throws TransformerException
    {
        MuleMessage msg = createMessage(METHOD_POST, "application/json");
        transform(msg);
    }

    private Object transform(MuleMessage msg) throws TransformerException
    {
        HttpRequestBodyToParamMap transformer = new HttpRequestBodyToParamMap();
        return transformer.transformMessage(msg, "UTF-8");
    }

    private void verifyTransformation(Object payload) throws TransformerException
    {
        assertThat(payload instanceof Map, is(true));
        Map<String, String> map = (Map<String, String>) payload;
        assertThat(map.size(), is(2));
        assertThat(map.get("key1"), is("value1"));
        assertThat(map.get("key2"), is("value2"));
    }

    private MuleMessage createMessage(String method, String contentType)
    {
        Map<String, Object> inboundProperties = new HashMap<String, Object>();
        inboundProperties.put("http.method", method);
        inboundProperties.put(HEADER_CONTENT_TYPE, contentType);

        String payload = "key1=value1&key2=value2";
        if ("GET".equals(method))
        {
            payload = "http://localhost/?" + payload;
        }
        MuleMessage msg = new DefaultMuleMessage(payload, inboundProperties, null, null, muleContext)
        {
            @Override
            public String getPayloadAsString(String encoding) throws Exception
            {
                return super.getPayload().toString();
            }

            @Override
            public byte[] getPayloadAsBytes() throws Exception
            {
                return ((String) super.getPayload()).getBytes();
            }
        };
        return msg;
    }

}
